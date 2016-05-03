package codegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fizzed.rocker.Rocker;

import codegen.util.SwaggerUtil;
import codegen.util.jaxb.ClientType;
import codegen.util.jaxb.ServerType;
import codegen.util.jaxb.SwaggerType;
import de.crunc.jackson.datatype.vertx.VertxJsonModule;
import io.vertx.core.json.JsonObject;

public class Generator {

	public void generate(SwaggerType swConfig) throws IOException {
		StringBuilder sbClient = new StringBuilder();
		ClientType client = swConfig.getClient();
		String packageName = swConfig.getServer().getPackageName();
		String destinationFolder = swConfig.getServer().getTargetFolder() + packageName.replace(".", "/");
		String destinationModelFolder = destinationFolder + "/model/";
		String destinationServiceFolder = destinationFolder + "/service/";
		FileWriter writer = null;
		File destFolder = null;

		try {
			YAMLFactory factory = new YAMLFactory();
			JsonParser parser = factory.createParser(new File(swConfig.getSchemaPath()));

			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new VertxJsonModule());

			final JsonObject schemaObject = mapper.readValue(parser, JsonObject.class);
			Map<String, Map<String, JsonObject>> groupPaths = SwaggerUtil.groupPaths(schemaObject);
			JsonObject definitions = schemaObject.getJsonObject("definitions");

			// server generation
			for (String groupName : groupPaths.keySet()) {
				String fileName = SwaggerUtil.firstCharUpperCase(groupName) + "Service";
				String serviceOutput = Rocker.template("views/swagger/qbit/server/Service.rocker.html", packageName,
						fileName, groupName, groupPaths.get(groupName), schemaObject).render().toString();
				destFolder = new File(destinationServiceFolder);
				if (destFolder.exists() == false) {
					destFolder.mkdirs();
				}
				writer = new FileWriter(destinationServiceFolder + fileName + ".java");
				writer.write(serviceOutput);
				writer.close();
			}
			for (Entry<String, Object> def : definitions) {
				String modelOutput = Rocker.template("views/swagger/qbit/server/Model.rocker.html", packageName,
						def.getKey(), (JsonObject) def.getValue()).render().toString();
				destFolder = new File(destinationModelFolder);
				if (destFolder.exists() == false) {
					destFolder.mkdirs();
				}
				String fileName = SwaggerUtil.firstCharUpperCase(def.getKey());
				writer = new FileWriter(destinationModelFolder + fileName + ".java");
				System.out.println(destinationModelFolder + fileName + ".java");
				writer.write(modelOutput);
				writer.close();
			}

			// client generation
			String[] references = client.getReferences().split("\n");
			String rootUrl = client.getRootUrl();
			String optionsOutput = Rocker
					.template("views/swagger/qbit/client/typescript/ClientApiOptions.rocker.html", references, rootUrl)
					.render().toString();
			sbClient.append(optionsOutput);
			for (Entry<String, Object> def : definitions) {
				String modelOutput = Rocker.template("views/swagger/qbit/client/typescript/ModelInterface.rocker.html",
						def.getKey(), (JsonObject) def.getValue()).render().toString();
				sbClient.append(modelOutput);
				sbClient.append("\r\n");
			}
			sbClient.append("\r\n");
			for (String groupName : groupPaths.keySet()) {
				String serviceOutput = Rocker.template("views/swagger/qbit/client/typescript/ServiceClient.rocker.html",
						packageName, groupName, groupPaths.get(groupName)).render().toString();
				sbClient.append(serviceOutput);
				sbClient.append("\r\n");
			}

			destFolder = new File(client.getTarget()).getParentFile();
			if (destFolder.exists() == false) {
				destFolder.mkdirs();
			}
			writer = new FileWriter(client.getTarget());
			System.out.println(client.getTarget());
			writer.write(sbClient.toString());
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (writer != null) {
				writer.close();
			}
		}

	}

}
