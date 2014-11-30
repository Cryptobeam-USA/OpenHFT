/*
 * Copyright 2014 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.lang.io.serialization.impl;

import net.openhft.lang.io.serialization.BytesMarshallable;
import net.openhft.lang.io.serialization.BytesMarshaller;
import net.openhft.lang.io.serialization.BytesMarshallerFactory;
import net.openhft.lang.io.serialization.CompactBytesMarshaller;
import net.openhft.lang.model.constraints.NotNull;

import java.io.Externalizable;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.openhft.lang.io.serialization.CompactBytesMarshaller.*;

/**
 * @author peter.lawrey
 */
public final class VanillaBytesMarshallerFactory implements BytesMarshallerFactory {
    private static final long serialVersionUID = 1L;

    private transient Map<Class, BytesMarshaller> marshallerMap;
    private transient BytesMarshaller[] compactMarshallerMap;

    private void init() {
        marshallerMap = new LinkedHashMap<>();
        compactMarshallerMap = new BytesMarshaller[256];
        BytesMarshaller stringMarshaller = new StringMarshaller(16 * 1024);
        addMarshaller(String.class, stringMarshaller);
        addMarshaller(CharSequence.class, stringMarshaller);
        addMarshaller(Class.class, new ClassMarshaller(Thread.currentThread().getContextClassLoader()));
        addMarshaller(Date.class, new DateMarshaller(10191));
        addMarshaller(Integer.class, new CompactEnumBytesMarshaller<>(Integer.class, 10191, INT_CODE));
        addMarshaller(Long.class, new CompactEnumBytesMarshaller<>(Long.class, 10191, LONG_CODE));
        addMarshaller(Double.class, new CompactEnumBytesMarshaller<>(Double.class, 10191, DOUBLE_CODE));
        addMarshaller(ByteBuffer.class, ByteBufferMarshaller.INSTANCE);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    public <E> BytesMarshaller<E> acquireMarshaller(@NotNull Class<E> eClass, boolean create) {
        if (marshallerMap == null) init();
        BytesMarshaller em = marshallerMap.get(eClass);
        if (em == null)
            if (eClass.isEnum())
                marshallerMap.put(eClass, em = new EnumBytesMarshaller(eClass, null));
            else if (BytesMarshallable.class.isAssignableFrom(eClass))
                marshallerMap.put(eClass, em = new BytesMarshallableMarshaller((Class) eClass));
            else if (Externalizable.class.isAssignableFrom(eClass))
                marshallerMap.put(eClass, em = new ExternalizableMarshaller((Class) eClass));
            else {
                try {
                    marshallerMap.put(eClass, em = new GenericEnumMarshaller<E>(eClass, 1000));
                } catch (Exception e) {
                    marshallerMap.put(eClass, em = NoMarshaller.INSTANCE);
                }
            }
        return em;
    }

    @Override
    public <E> BytesMarshaller<E> getMarshaller(byte code) {
        if (marshallerMap == null) init();
        return compactMarshallerMap[code & 0xFF];
    }

    public <E> void addMarshaller(Class<E> eClass, BytesMarshaller<E> marshaller) {
        if (marshallerMap == null) init();
        marshallerMap.put(eClass, marshaller);
        if (marshaller instanceof CompactBytesMarshaller)
            compactMarshallerMap[((CompactBytesMarshaller) marshaller).code()] = marshaller;
    }
}
