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
import java.util.Collection;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Nashorn implementation for viz.js. At the moment not active since it is
 * awfully slow with bigger .dot files.
 * 
 * @author Andreas Studer
 *
 */
class NashornVizJS extends VizJS {
	// JS variable to collect messages form viz.js in case of fatal errors.
	private static final String JSMESSAGESARRAY = "messages";
	// JS array initialization.
	private static final String JSMESSAGESARRAYINIT = "messages=[];";
	// Overwrite JS print function for collecting messages out of viz.js.
	private static final String JSPRINTFUNCTION = "var messages=[]; print=function(s){messages.push(s);};";

	private ScriptEngine engine;
	private Invocable invoke;

	/**
	 * Initialization of Nashorn scripting engine.
	 * ScriptExceptions will be wrapped into VizJSException.
	 * @throws IOException
	 */
	public NashornVizJS() throws IOException {

		engine = new ScriptEngineManager().getEngineByName("nashorn");
		try {
			engine.eval(JSPRINTFUNCTION);
			engine.eval(getVizCode());
		} catch (ScriptException e) {
			throw new VizJSException("Initializing of Nashorn failed (script error).", e);
		}
		invoke = (Invocable) engine;
	}

	@Override
	public String execute(String dot) {
		try {
			engine.eval(JSMESSAGESARRAYINIT);
			return invoke.invokeFunction("Viz", dot).toString();
		} catch (NoSuchMethodException | ScriptException e) {
			try {
				@SuppressWarnings("unchecked")
				Collection<Object> messages = ((Map<?, Object>) engine.eval(JSMESSAGESARRAY)).values();
				if (messages != null && !messages.isEmpty()) {
					// Now something really bad happened: viz.js tells us
					// something
					// with printing (e.g. Abort).
					String summary = "";
					for (Object line : messages) {
						summary += line + "\n";
					}
					throw new VizJSException(summary, e);
				}
			} catch (ScriptException e1) {
				throw new VizJSException("Problems getting messages in engine: " + getVersion(), e1);
			}
			throw new VizJSException("Problems executing function viz.js in engine: " + getVersion(), e);
		}
	}

	@SuppressWarnings("static-access")
	@Override
	public String getVersion() {
		return engine.ENGINE + " " + engine.ENGINE_VERSION;
	}

	@Override
	protected void release() {
		// Nothing to release for Nashorn.
	}

}
