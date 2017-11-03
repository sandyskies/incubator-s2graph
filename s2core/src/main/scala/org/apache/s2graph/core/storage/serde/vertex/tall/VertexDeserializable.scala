/*
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

package org.apache.s2graph.core.storage.serde.vertex.tall

import org.apache.s2graph.core.mysqls.ColumnMeta
import org.apache.s2graph.core.storage.CanSKeyValue
import org.apache.s2graph.core.storage.serde.Deserializable
import org.apache.s2graph.core.storage.serde.StorageDeserializable._
import org.apache.s2graph.core.types.{HBaseType, InnerValLike, VertexId}
import org.apache.s2graph.core.{S2Graph, S2Vertex, S2VertexLike}

class VertexDeserializable(graph: S2Graph,
                           bytesToInt: (Array[Byte], Int) => Int = bytesToInt) extends Deserializable[S2VertexLike] {
  def fromKeyValues[T: CanSKeyValue](_kvs: Seq[T],
                                          cacheElementOpt: Option[S2VertexLike]): Option[S2VertexLike] = {
    try {
      assert(_kvs.size == 1)

      val kvs = _kvs.map { kv => implicitly[CanSKeyValue[T]].toSKeyValue(kv) }
      val kv = kvs.head
      val version = HBaseType.DEFAULT_VERSION
      val (vertexId, _) = VertexId.fromBytes(kv.row, 0, kv.row.length, version)
      val serviceColumn = vertexId.column
      val schemaVer = serviceColumn.schemaVersion

      val (props, _) = bytesToKeyValues(kv.value, 0, kv.value.length, schemaVer, serviceColumn)

      val propsMap = new collection.mutable.HashMap[ColumnMeta, InnerValLike]
      props.foreach { case (columnMeta, innerVal) =>
        propsMap += (columnMeta -> innerVal)
      }

      val vertex = graph.newVertex(vertexId, kv.timestamp, S2Vertex.EmptyProps, belongLabelIds = Nil)
      S2Vertex.fillPropsWithTs(vertex, propsMap.toMap)

      Option(vertex)
    } catch {
      case e: Exception => None
    }
  }
}
