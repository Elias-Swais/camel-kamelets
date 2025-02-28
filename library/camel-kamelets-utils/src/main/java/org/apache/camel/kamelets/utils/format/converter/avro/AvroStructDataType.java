/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.camel.kamelets.utils.format.converter.avro;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.kamelets.utils.format.MimeType;
import org.apache.camel.kamelets.utils.format.converter.utils.SchemaHelper;
import org.apache.camel.kamelets.utils.format.spi.DataTypeConverter;
import org.apache.camel.kamelets.utils.format.spi.annotations.DataType;

/**
 * Data type uses Avro Jackson data format to unmarshal Exchange body to generic JsonNode.
 * Uses given Avro schema from the Exchange properties when unmarshalling the payload (usually already resolved via schema
 * resolver Kamelet action).
 */
@DataType(name = "avro-x-struct", mediaType = "application/x-struct")
public class AvroStructDataType implements DataTypeConverter {

    @Override
    public void convert(Exchange exchange) {
        AvroSchema schema = exchange.getProperty(SchemaHelper.CONTENT_SCHEMA, AvroSchema.class);

        if (schema == null) {
            throw new CamelExecutionException("Missing proper avro schema for data type processing", exchange);
        }

        try {
            Object unmarshalled = Avro.MAPPER.reader().forType(JsonNode.class).with(schema)
                    .readValue(getBodyAsStream(exchange));
            exchange.getMessage().setBody(unmarshalled);

            exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, MimeType.STRUCT.type());
        } catch (InvalidPayloadException | IOException e) {
            throw new CamelExecutionException("Failed to apply Avro x-struct data type on exchange", exchange, e);
        }
    }

    private InputStream getBodyAsStream(Exchange exchange) throws InvalidPayloadException {
        InputStream bodyStream = exchange.getMessage().getBody(InputStream.class);

        if (bodyStream == null) {
            bodyStream = new ByteArrayInputStream(exchange.getMessage().getMandatoryBody(byte[].class));
        }

        return bodyStream;
    }
}
