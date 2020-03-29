package com.jms;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProcessorTransformerSample {
    // private final static Logger logger =
    // LoggerFactory.getLogger(WeeklyProcess.class);

    public static void main(final String[] args) {
        SpringApplication.run(ProcessorTransformerSample.class, args);
    }

    public static class ProcessorTransformer {

        KeyValueStore<String, Long> state = null;

        @Bean
        public Function<KStream<String, String>, KStream<String, Long>> inputwordsworking() {

            return input -> input.flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                    .map((key, value) -> new KeyValue<>(value, value))
                    .groupByKey(Grouped.with(Serdes.String(), Serdes.String())).count(Materialized.as("mystate2"))
                    .toStream();
        }

        @Bean
        public Function<KStream<String, String>, KStream<String, Long>> inputwords() {

            return input -> input.flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                    .map((key, value) -> new KeyValue<>(value, value))
                    .groupByKey(Grouped.with(Serdes.String(), Serdes.String())).count().toStream()
                    .peek((key, value) -> System.out.println("****inputwords*****key=" + key + ", value=" + value));
        }

        @Bean
        public Consumer<KStream<String, Long>> processortest() {
            return input -> input.process(() -> new Processor<String, Long>() {

                @Override
                public void init(ProcessorContext context) {
                    System.out.println("#############processortest: init>>>>>>>>>>>>>>>>>>>");
                    if (state == null) {
                        System.out.println("#############processortest: get state its nulll>>>>>>>>>>>>>>>>>>>");
                        state = (KeyValueStore<String, Long>) context.getStateStore("mystate");

                    } else {

                        System.out.println("#############processortest: init.state NOT NULL");
                    }
                }

                @Override
                public void process(String key, Long value) {
                    System.out.println("#############processortest: process>>>>>>>>>>>>>>>>>>> key:: " + key
                            + " value:: " + value);
                    if (state != null) {

                        if (key != null) {
                            Long currentCount = state.get(key);

                            if (currentCount == null) {
                                // state.put(key, value);
                                state.put(key, value);
                                System.out.println("#############processortest:No value found for key : " + key);
                            } else {
                                // state.put(key, currentCount + value);
                                state.put(key, value);
                                System.out.println(
                                        "#############processortest: process>>>>>>>>> current value:: for key: " + key
                                                + " :" + currentCount);
                            }

                            System.out
                                    .println("#############processortest: process>>>>>>>>>>> updated value:: for key: "
                                            + key + " : " + state.get(key));
                        } else
                            System.out.println("NULL KEY :(((((((((((((((())))))))))))))))");

                    } else
                        System.out.println("NULL STATE :(((((((((((((((())))))))))))))))");

                    // business logic
                }

                @Override
                public void close() {
                    System.out.println("@@@@@@@@@@@@@@@@@processortest: close>>>>>>>>>>>>>>>>>>>>>>>>>");
                    if (state != null) {
                        state.close();
                    }

                }
            }, "mystate");

        }

        @Bean
        public Function<KStream<String, Long>, KStream<String, Long>> transformertest() {
            return (input) -> input.transform(() -> new Transformer<String, Long, KeyValue<String, Long>>() {

                @Override
                public void init(ProcessorContext context) {
                    System.out.println("############# transformertest: init ******************");
                    if (state == null) {
                        state = (KeyValueStore<String, Long>) context.getStateStore("mystate");
                    }

                }

                @Override
                public void close() {
                    System.out.println("transformertest: close *************");
                    if (state != null) {
                        state.close();

                    }
                }

                @Override
                public KeyValue<String, Long> transform(String key, Long value) {
                    // business logic - return transformed KStream;
                    KeyValue<String, Long> kv = null;

                    System.out.println("#############transformertest: transform >>>>>>>>>>>>>>>>>>> key:: " + key
                            + " value:: " + value);
                    if (state != null) {

                        if (key != null) {
                            Long currentCount = state.get(key);

                            if (currentCount == null) {
                                state.put(key, value);
                                System.out.println(
                                        "#############transformertest:transform   No value found for key : " + key);
                                kv = new KeyValue<String, Long>(key, value);
                            } else {
                                // state.put(key, currentCount + value);
                                state.put(key, value);
                                kv = new KeyValue<String, Long>(key, currentCount + value);
                                System.out.println(
                                        "#############transformertest: transform >>>>>>>>> current value:: for key: "
                                                + key + " :" + currentCount);
                            }

                            System.out.println(
                                    "#############transformertest: transform >>>>>>>>>>> updated value:: for key: "
                                            + key + " : " + state.get(key));
                        } else
                            System.out.println("NULL KEY :(((((((((((((((())))))))))))))))");

                    } else
                        System.out.println("NULL STATE :(((((((((((((((())))))))))))))))");

                    return kv;
                }

            }, "mystate");

        }

        @Bean
        public StoreBuilder<KeyValueStore<String, Long>> myStore() {
            return Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore("mystate"), Serdes.String(),
                    Serdes.Long());
        }

    }
}
