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

import java.io.IOException;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.utils.V8ObjectUtils;

/**
 * Create a wrapper with J2V8 and viz.js.
 * 
 * @author Andreas Studer
 *
 */
class V8VizJS extends VizJS {
	// JS variable to collect messages form viz.js in case of fatal errors.
	private static final String JSMESSAGESARRAY = "messages";
	// Splice JS array to reduce size.
	private static final String JSMESSAGESARRAYSPLICE = "messages.splice(0, 100);";
	// Overwrite JS print function for collecting messages out of viz.js.
	private static final String JSPRINTFUNCTION = "var messages=[]; print=function(s){messages.push(s);};";

	private V8 runtime;
	private V8Array messages;
	private V8Function vizFunction;

	/**
	 * creates and initializes V8 engine with viz.js
	 * 
	 * @throws IOException
	 */
	public V8VizJS() throws IOException {
		runtime = V8.createV8Runtime();
		runtime.executeVoidScript(JSPRINTFUNCTION);
		messages = runtime.getArray(JSMESSAGESARRAY);
		runtime.executeVoidScript(getVizCode());
		vizFunction = (V8Function) runtime.getObject("Viz");
	}

	@Override
	public String execute(String dot) {
		V8Array parameters = new V8Array(runtime).push(dot);
		try {
			runtime.executeVoidScript(JSMESSAGESARRAYSPLICE);
			return (String) vizFunction.call(runtime, parameters);
		} catch (Exception e) {

			if (messages.length() > 0) {
				// Now something really bad happened: viz.js tells us something
				// with printing (e.g. Abort).
				String summary = "";
				for (int i = 0; i < messages.length(); i++) {
					summary += V8ObjectUtils.getValue(messages, i) + "\n";
				}
				throw new VizJSException(summary, e);
			}

			throw new VizJSException("Problems executing function viz.js in engine: " + getVersion(), e);
		} finally {
			parameters.release();
		}
	}

	@Override
	public void release() {
		vizFunction.release();
		messages.release();
		runtime.release(true);
	}

	@Override
	public String getVersion() {
		return "J2V8 build: " + runtime.getBuildID();
	}
}
