package com.odiousapps.weewxweather;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class JsBridge
{
/* noinspection
	private final CountDownLatch latch;
	private final String[] htmlHolder;
	private final SafeWebView webView;

	static final String javascript = """
(function(){
  try {
    if(window.__android_detector_installed){ console.log('detector: already installed'); return; }
    console.log('detector: start');
    window.__android_detector_installed = true;

    window.__android_pending = 0;

    (function(open, send){
      XMLHttpRequest.prototype.open = function(){ this._url = arguments[1]; return open.apply(this, arguments); };
      XMLHttpRequest.prototype.send = function(){
        window.__android_pending++;
        var xhr = this;
        function done(){
          try{
            if(!xhr.__android_done){
              xhr.__android_done = true;
              window.__android_pending--;
            }
          }catch(e){}
        }
        xhr.addEventListener('load', done);
        xhr.addEventListener('error', done);
        xhr.addEventListener('abort', done);
        xhr.addEventListener('timeout', done);
        return send.apply(this, arguments);
      };
    })(XMLHttpRequest.prototype.open, XMLHttpRequest.prototype.send);

    if(window.fetch){
      var _fetch = window.fetch;
      window.fetch = function(){
        window.__android_pending++;
        return _fetch.apply(this, arguments)
          .then(function(r){ window.__android_pending--; return r; })
          .catch(function(e){ window.__android_pending--; throw e; });
      };
    }

    var lastChange = Date.now();
    var mo = new MutationObserver(function(){ lastChange = Date.now(); });
    mo.observe(document, { childList:true, subtree:true, attributes:true, characterData:true });

    function sendHtml(note){
      try {
        var html = document.documentElement ? document.documentElement.outerHTML : '';
        console.log('detector: sending html, len=' + html.length + ', note=' + note);
        if(window.AndroidBridge && window.AndroidBridge.onReady)
          AndroidBridge.onReady(html);
      } catch(e){
        console.log('detector: send failed '+e);
      }
    }

    var stableMs = 600;
    var pollInterval = 250;
    var maxWait = 15000;
    var minLength = 1024;
    var start = Date.now();

    (function check(){
      try
      {
        var now = Date.now();
        var pending = window.__android_pending || 0;

        var html = "";
        try
        {
            html = document.documentElement ? document.documentElement.outerHTML : "";
        } catch(e) {
            console.log('detector top error:'+e);
        }

        var htmlLen = html.length;

        // Only send when stable, loaded, and long enough
        if (pending === 0 &&
            (now - lastChange) > stableMs &&
            htmlLen >= minLength)
        {
          console.log('detector: stable & length ok ('+htmlLen+')');
          sendHtml('stable');
          mo.disconnect();
          return;
        }

        // timeout → send whatever we have
        if ((now - start) > maxWait){
          console.log('detector: timeout, htmlLen='+htmlLen);
          sendHtml('timeout');
          mo.disconnect();
          return;
        }

        console.log('detector: waiting… pending='+pending+
                    ' stable='+(now-lastChange)+
                    ' htmlLen='+htmlLen);

      } catch(e){
        console.log('detector: check err '+e);
      }
      setTimeout(check, pollInterval);
    })();

    // Fallback after 2s ONLY if long enough
    setTimeout(function(){
      try {
        var html = document.documentElement ? document.documentElement.outerHTML : "";
        var htmlLen = html.length;
        if(window.__android_pending===0 && htmlLen >= minLength)
        {
          console.log('detector: fallback triggered (len='+htmlLen+')');
          sendHtml('fallback');
          mo.disconnect();
        }
      } catch(e) {
        console.log('detector top error:'+e);
      }
    }, 2000);

  } catch(ex) {
    console.log('detector top error:'+ex);
  }
})();""";

	public JsBridge(CountDownLatch latch, String[] htmlHolder, SafeWebView webView)
	{
		this.latch = latch;
		this.htmlHolder = htmlHolder;
		this.webView = webView;
	}

	@JavascriptInterface
	public void onReady(String html)
	{
		weeWXAppCommon.LogMessage("Got the following HTML length: " + html.length());

		// This runs on the UI thread internally, but be careful with heavy work
		htmlHolder[0] = cleanReturnedHtml(html);
		//Common.LogMessage("Got the following HTML: " + newhtml);

		latch.countDown();
	}

	static String cleanReturnedHtml(String jsonEncodedHtml)
	{
		if (jsonEncodedHtml == null || jsonEncodedHtml.isBlank())
			return null;

		String s = jsonEncodedHtml.strip();

		// remove surrounding quotes if present
		if(s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2)
			s = s.substring(1, s.length() - 1);

		// common escape sequences returned by evaluateJavascript
		s = s.replace("\\u003C", "<");
		s = s.replace("\\n", "\n");
		s = s.replace("\\\"", "\"");
		s = s.replaceAll("\\\\r", "\r");

		return s;
	}

	static String escapeJsString(String s)
	{
		if (s == null)
			return "";

		return s.replace("\\", "\\\\")
				.replace("'", "\\'")
				.replace("\"", "\\\"");
	}
*/
}