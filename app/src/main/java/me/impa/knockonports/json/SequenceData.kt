/*
 * Copyright (c) 2018 Alexander Yaburov
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package me.impa.knockonports.json

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import me.impa.knockonports.database.entity.Sequence
import java.io.StringReader
import java.io.StringWriter

data class SequenceData(var name: String?, var host: String?, var timeout: Int?, var delay: Int?,
                        var udpContent: String?, var application: String?, var base64: Int?,
                        var ports: List<PortData>) {

    fun toEntity(): Sequence = Sequence(null, name, host, timeout, null, delay, udpContent, application, base64,
            ports.filter { it.value != null }.joinToString(", ") { it.toString() })

    companion object {
        fun fromEntity(sequence: Sequence): SequenceData =
                SequenceData(sequence.name, sequence.host, sequence.timeout, sequence.delay, sequence.udpContent,
                        sequence.application, sequence.base64, sequence.getPortList())

        private fun writeValue(writer: JsonWriter, name: String, value: String?) {
            if (value == null)
                writer.name(name).nullValue()
            else
                writer.name(name).value(value)
        }

        private fun writeValue(writer: JsonWriter, name: String, value: Int?) {
            if (value == null)
                writer.name(name).nullValue()
            else
                writer.name(name).value(value)
        }

        private fun readString(reader: JsonReader): String? {
            return if (reader.peek() == JsonToken.NULL) {
                reader.nextNull(); null
            } else {
                reader.nextString()
            }
        }

        private fun readInt(reader: JsonReader): Int? {
            return if (reader.peek() == JsonToken.NULL) {
                reader.nextNull(); null
            } else {
                reader.nextInt()
            }
        }

        fun toJson(sequenceList: List<SequenceData>): String {
            val sw = StringWriter()
            val writer = JsonWriter(sw)
            try {
                writer.beginArray()

                sequenceList.forEach {
                    writer.beginObject()
                    writeValue(writer, "name", it.name)
                    writeValue(writer, "host", it.host)
                    writeValue(writer, "timeout", it.timeout)
                    writeValue(writer, "delay", it.delay)
                    writeValue(writer, "udp_content", it.udpContent)
                    writeValue(writer, "application", it.application)
                    writeValue(writer, "base64", it.base64)
                    writer.name("ports")
                    writer.beginArray()
                    it.ports.forEach { p ->
                        writer.beginObject()
                        writeValue(writer, "value", p.value)
                        writeValue(writer, "type", p.type)
                        writer.endObject()
                    }
                    writer.endArray()
                    writer.endObject()
                }

                writer.endArray()
                return sw.toString()
            }
            finally {
                writer.close()
                sw.close()
            }
        }

        fun fromJson(input: String?): List<SequenceData> {
            input ?: return listOf()

            val result = mutableListOf<SequenceData>()

            val sr = StringReader(input)
            val reader = JsonReader(sr)

            try {
                reader.beginArray()
                while (reader.hasNext()) {
                    reader.beginObject()
                    val seq = SequenceData(null, null, null, null, null, null, null, listOf())
                    val ports = mutableListOf<PortData>()
                    while (reader.hasNext()) {
                        val key = reader.nextName()
                        when(key) {
                            "name" -> seq.name = readString(reader)
                            "host" -> seq.host = readString(reader)
                            "timeout" -> seq.timeout = readInt(reader)
                            "delay" -> seq.delay = readInt(reader)
                            "udp_content" -> seq.udpContent = readString(reader)
                            "application" -> seq.application = readString(reader)
                            "base64" -> seq.base64 = readInt(reader)
                            "ports" -> {
                                reader.beginArray()
                                while (reader.hasNext()) {
                                    reader.beginObject()
                                    while (reader.hasNext()) {
                                        val port = PortData(null, 0)
                                        while (reader.hasNext()) {
                                            val portKey = reader.nextName()
                                            when(portKey) {
                                                "value" -> port.value = readInt(reader)
                                                "type" -> port.type = readInt(reader) ?: 0
                                            }
                                        }
                                        ports.add(port)
                                    }
                                    reader.endObject()
                                }
                                reader.endArray()
                            }
                        }
                    }
                    seq.ports = ports
                    result.add(seq)
                    reader.endObject()
                }
                reader.endArray()
            }
            finally {
                reader.close()
                sr.close()
            }

            return result
        }
    }
}