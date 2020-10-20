package com.navercorp.pinpoint.plugin.kafka;

import com.navercorp.pinpoint.pluginit.utils.AgentPath;
import com.navercorp.pinpoint.test.plugin.*;
import org.junit.runner.RunWith;


/**
 * @author Younsung Hwang
 */
@RunWith(PinpointPluginTestSuite.class)
@PinpointAgent(AgentPath.PATH)
@PinpointConfig("pinpoint-kafka-client.config")
@ImportPlugin({"com.navercorp.pinpoint:pinpoint-kafka-plugin"})
@Dependency({
        "org.apache.kafka:kafka_2.12:[1.0.0]",
        "log4j:log4j:[1.2.17]",
        "info.batey.kafka:kafka-unit:[1.0]",
        "org.apache.kafka:kafka-clients:[1.0.0],[1.0.1],[1.0.2]",
})
@JvmVersion(8)
public class KafkaClient_1_0_x_IT extends KafkaClientITBase {

}
