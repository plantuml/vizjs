/* Licensed under the Revised BSD License (the Revised Berkeley Software Distribution)
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * * Neither the name of the University of California, Berkeley nor the
 *   names of its contributors may be used to endorse or promote products
 *   derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 *
 * Original Author:  Andreas Studer
 */
package ch.braincell.viz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Simple access class for viz.js; either J2V8 if available for the platform or
 * Nashorn (JRE8).
 * 
 * @author Andreas Studer
 *
 */
public abstract class VizJS {

	/**
	 * creates an instance of the wrapper with the JS-engine. Every thread needs
	 * one engine instance. It's not possible to use one instance in multiple
	 * treads.
	 * 
	 * @return JS-engine wrapper, pre-loaded with viz.js
	 */
	public static VizJS create() {
		try {
			return new V8VizJS();
		} catch (IOException e) {
			throw new VizJSException("Loading of viz.js resource in .jar failed!", e);
		}
	}

	/**
	 * Execute dot string.
	 * 
	 * @param dot
	 *            definition in the dot language (see GraphViz).
	 * @return svg presentation.
	 */
	public abstract String execute(String dot);

	/**
	 * Returns engine initiated.
	 * @return engine description
	 */
	public abstract String getVersion();
	
	/**
	 * Release all resources kept by the JavaScript engine. This is mainly
	 * important for native bound engines (e.g. J2V8).
	 */
	protected abstract void release();

	/**
	 * Close resources if class gets finalized through GC (Not sure if this is
	 * the right way to do though).
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		release();
		System.out.println("released...");
	};

	/**
	 * Load the viz.js library as String.
	 * 
	 * @return viz.js as String
	 * @throws IOException
	 *             Exception if viz.js as resource is not found in classpath.
	 */
	protected String getVizCode() throws IOException {
		try (final BufferedReader read = new BufferedReader(
				new InputStreamReader(getClass().getResourceAsStream("/javascript/viz-1.3.0.js"), "UTF-8"))) {
			return read.lines().collect(Collectors.joining("\n"));
		}
	}
}
