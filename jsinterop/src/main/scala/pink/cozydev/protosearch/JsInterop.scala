/*
 * Copyright 2022 CozyDev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pink.cozydev.protosearch

import scala.scalajs.js.annotation._
import scala.scalajs.js
import org.scalajs.dom.Blob
import pink.cozydev.protosearch.analysis.QueryAnalyzer
import pink.cozydev.protosearch.analysis.Analyzer
import scala.collection.mutable.ArrayBuffer
import scodec.bits.ByteVector

@JSExportTopLevel("Hit")
class Hit(
    val id: Int,
    val score: Double,
) extends js.Object

@JSExportTopLevel("Querier")
class Querier(val mIndex: MultiIndex, val defaultField: String) {
  import js.JSConverters._

  private val scorer = Scorer(mIndex)
  private val qAnalyzer = QueryAnalyzer(
    defaultField,
    (defaultField, Analyzer.default),
  )
  @JSExport
  def search(query: String): js.Array[Hit] = {
    val hits = qAnalyzer
      .parse(query)
      .flatMap(q => mIndex.search(q.qs).flatMap(ds => scorer.score(q.qs, ds.toSet)))
      .map(hs => hs.map(new Hit(_, _)))
      .toOption
      .getOrElse(Nil)
    hits.toJSArray
  }
}

@JSExportTopLevel("QuerierBuilder")
object QuerierBuilder {
  private def decode(buf: js.typedarray.ArrayBuffer): MultiIndex = {
    val bv = ByteVector.fromJSArrayBuffer(buf)
    MultiIndex.codec.decodeValue(bv.bits).require
  }

  @JSExport
  def load(bytes: Blob, defaultField: String): js.Promise[Querier] =
    bytes.arrayBuffer().`then` { buf =>
      val mIndex = decode(buf)
      new Querier(mIndex, defaultField)
    }

}
