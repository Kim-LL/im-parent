package com.paas.im.codec;

import com.paas.im.model.pojo.KafkaTransportObject;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

public class KafkaSerializer implements Serializer<KafkaTransportObject>{

    @Override
    public byte[] serialize(String topic, KafkaTransportObject data) {
        if(data == null) {
            return null;
        }
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(data);
            oos.flush();
            return bos.toByteArray();
        }catch (Exception e) {
            throw new SerializationException(
                    "Error when deserializing Package to byte[]  due to exception. " + data);
        }
    }
}
