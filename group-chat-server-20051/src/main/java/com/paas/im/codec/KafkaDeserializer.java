package com.paas.im.codec;

import com.paas.im.model.pojo.KafkaTransportObject;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;

public class KafkaDeserializer implements Deserializer<KafkaTransportObject> {

    @Override
    public KafkaTransportObject deserialize(String topic, byte[] data) {
        KafkaTransportObject res = null;
        try {
            if (data == null) {
                return null;
            }
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bis);
            res = (KafkaTransportObject) ois.readObject();
            ois.close();
            bis.close();
        } catch (Exception e){
            throw new SerializationException(
                    "Error when deserializing Package to byte[]  due to exception. " + Arrays.toString(data));
        }
        return res;
    }
}
