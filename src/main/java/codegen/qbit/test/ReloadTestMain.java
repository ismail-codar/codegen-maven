package codegen.qbit.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fizzed.rocker.Rocker;
import com.fizzed.rocker.runtime.RockerRuntime;

import codegen.util.SwaggerUtil;
import de.crunc.jackson.datatype.vertx.VertxJsonModule;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ReloadTestMain extends AbstractVerticle {

	public static void main(String[] args) {
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ReloadTestMain());

        System.out.println("Started ReloadTestMain");
	}

	@Override
	public void start() throws Exception {
		final Router router = Router.router(vertx);
		YAMLFactory factory = new YAMLFactory();
		JsonParser parser = factory.createParser(new File("./src/main/resources/petstore.yaml")); 

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new VertxJsonModule());

		final JsonObject jsonObject  = mapper.readValue(parser, JsonObject.class);

//		serviceExample(null, jsonObject);
		
		RockerRuntime.getInstance().setReloading(true);
//		router.get().handler(ctx -> {
//			serviceExample(ctx, jsonObject);
//		});
		router.get().handler(new Handler<RoutingContext>() {
			
			@Override
			public void handle(RoutingContext ctx) {
				serviceExample(ctx, jsonObject);
			}
		});
		vertx.createHttpServer().requestHandler(router::accept).listen(8080);
	}

	private void serviceExample(RoutingContext ctx, JsonObject jsonObject) {
		Map<String, Map<String, JsonObject>> groupPaths = SwaggerUtil.groupPaths(jsonObject);
		
		JsonObject item;
		JsonArray arr;
		
//		item.getJsonArray("produses").getList().get(0).toString()
//		for (Entry<String, Object> entry : item) {
//			entry.getKey()
//		}
		
		String out = Rocker.template("views/swagger/qbit/server/Service.rocker.html", "dd", SwaggerUtil.firstCharUpperCase("users"), "users", groupPaths.get("users"), jsonObject).render().toString();
		ctx.response().end("<pre>" + out + "</pre>");
	}


	private void modelExample(RoutingContext ctx, JsonObject jsonObject) {
		String out = Rocker.template("views/swagger/qbit/server/Model.rocker.html", "dd", "User",
				jsonObject.getJsonObject("definitions").getJsonObject("User").getJsonObject("properties").getMap(), jsonObject).render().toString();
		ctx.response().end("<pre>" + out + "</pre>");
	}
}
