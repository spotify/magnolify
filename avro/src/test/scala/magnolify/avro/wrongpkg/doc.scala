/*
 * Copyright 2026 Spotify AB
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

package magnolify.avro.wrongpkg

import scala.annotation.StaticAnnotation

/**
 * A fake @doc annotation in a different package, simulating what happens
 * when users accidentally use @doc from com.spotify.scio.avro.types instead
 * of magnolify.shared.doc.
 */
class doc(value: String) extends StaticAnnotation
