import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.ekstep.common.util.TelemetryAccessEventUtil;
import org.ekstep.search.router.SearchRequestRouterPool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilimi.common.dto.Response;

import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.Logger.ALogger;
import play.core.j.JavaResultExtractor;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Result;

public class Global extends GlobalSettings {

	private static final ALogger accessLogger = Logger.of("accesslog");
	private static ObjectMapper mapper = new ObjectMapper();

	public void onStart(Application app) {
		SearchRequestRouterPool.init();
	}

	@SuppressWarnings("rawtypes")
	public Action onRequest(Request request, Method actionMethod) {
		long startTime = System.currentTimeMillis();
		return new Action.Simple() {
			public Promise<Result> call(Context ctx) throws Throwable {
				Promise<Result> call = delegate.call(ctx);
				call.onRedeem((r) -> {
					try {
						JsonNode requestData = request.body().asJson();
						com.ilimi.common.dto.Request req = mapper.convertValue(requestData,
								com.ilimi.common.dto.Request.class);
						byte[] body = JavaResultExtractor.getBody(r, 0l);
						Response responseObj = mapper.readValue(body, Response.class);
						
						Map<String,Object> data = new HashMap<String, Object>();
						data.put("StartTime", startTime);
						data.put("Request", req);
						data.put("Response", responseObj);
						data.put("RemoteAddress", request.remoteAddress());
						data.put("ContentLength", body.length);
						data.put("Status", r.status());
						data.put("Protocol", request.secure() ? "HTTPS" : "HTTP");
						data.put("Method", request.method());
						data.put("X-Session-ID", request.getHeader("X-Session-ID"));
						data.put("X-Consumer-ID", request.getHeader("X-Consumer-ID"));
						data.put("X-Device-ID", request.getHeader("X-Device-ID"));
						data.put("X-Authenticated-Userid", request.getHeader("X-Authenticated-Userid"));
						TelemetryAccessEventUtil.writeTelemetryEventLog(data);
						accessLogger.info(request.remoteAddress() + " " + request.host() + " " + request.method() + " "
								+ request.uri() + " " + r.status() + " " + body.length);
					} catch (Exception e) {
						accessLogger.error(e.getMessage());
					}
				});
				return call;
			}
		};
	}
}
