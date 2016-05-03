package codegen.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class SwaggerUtil {
	public static String firstCharUpperCase(String txt) {
		return txt.substring(0, 1).toUpperCase() + txt.substring(1);
	}

	public static Map<String, Map<String, JsonObject>> groupPaths(JsonObject jsonObject) {
		Set<String> keySet = jsonObject.getJsonObject("paths").getMap().keySet();
		Map<String, Map<String, JsonObject>> groups = new HashMap<String, Map<String, JsonObject>>();
		Map<String, JsonObject> pathMap = null;
		JsonObject paths = jsonObject.getJsonObject("paths");

		for (String pathKey : keySet) {
			int idx = pathKey.indexOf("/", 1);
			if (idx == -1)
				idx = pathKey.length();
			String groupKey = pathKey.substring(1, idx);

			if (groups.containsKey(groupKey) == false) {
				pathMap = new HashMap<String, JsonObject>();
				groups.put(groupKey, pathMap);
			} else {
				pathMap = groups.get(groupKey);
			}
			pathMap.put(pathKey.substring(groupKey.length() + 1), paths.getJsonObject(pathKey));
		}

		return groups;
	}

	// TODO dummy method
	public static String responseDescription(Map<String, Object> response) {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Object> entry : response.entrySet()) {
			sb.append(entry.getKey());
			sb.append(": ");
			sb.append(((JsonObject) entry.getValue()).getString("description"));
			sb.append(", ");
		}
		return sb.length() > 2 ? sb.toString().substring(0, sb.length() - 2) : sb.toString();
	}

	public static String responseTypeName(JsonObject response, String packageName, boolean forTypeScript) {
		JsonObject successResponse = response.getJsonObject("200");
		if (successResponse == null)
			successResponse = response.getJsonObject("default");
		if (successResponse != null) {
			return forTypeScript ? typeRefNameTs(successResponse) : typeRefName(successResponse, packageName);
		}
		return forTypeScript ? "void" : "Void";
	}

	public static String responseTypeParam(JsonObject response, String packageName) {
		return responseTypeName(response, packageName, true).equals("void") ? "" : "data";
	}

	public static String typeRefName(JsonObject jsonData, String packageName) {
		String typeName = "Void";
		if (jsonData != null) {
			JsonObject schema = jsonData.getJsonObject("schema");
			if (schema != null)
				jsonData = schema;
			String type = jsonData.getString("type");
			if (type != null) {
				if (jsonData.getJsonObject("items") != null) {
					StringBuilder sb = new StringBuilder();
					sb.append("java.util.List<");
					// TODO http://swagger.io/specification/#itemsObject
					sb.append(typeRefName(jsonData.getJsonObject("items"), packageName));
					sb.append(">");
					typeName = sb.toString();
				} else
					typeName = firstCharUpperCase(type);
			} else {
				String refName = jsonData.getString("$ref");
				if (refName != null)
					typeName = packageName + ".model." + refName.substring(refName.lastIndexOf("/") + 1);
			}
		}
		return typeName;
	}

	public static String typeRefNameTs(JsonObject jsonData) {
		String typeName = "void";
		if (jsonData != null) {
			JsonObject schema = jsonData.getJsonObject("schema");
			if (schema != null)
				jsonData = schema;
			String type = jsonData.getString("type");
			if (type != null) {
				if (jsonData.getJsonObject("items") != null) {
					StringBuilder sb = new StringBuilder();
					sb.append("Array<");
					// TODO http://swagger.io/specification/#itemsObject
					sb.append(typeRefNameTs(jsonData.getJsonObject("items")));
					sb.append(">");
					typeName = sb.toString();
				} else
					typeName = type;
			} else {
				String refName = jsonData.getString("$ref");
				if (refName != null)
					typeName = refName.substring(refName.lastIndexOf("/") + 1);
			}
		}
		if (typeName.equals("integer"))
			typeName = "number";
		return typeName;
	}
	
	public static boolean isSimpleType(JsonObject response) {
		JsonObject jsonData = response.getJsonObject("200");
		if (jsonData == null)
			jsonData = response.getJsonObject("default");
		if (jsonData != null) {
			JsonObject schema = jsonData.getJsonObject("schema");
			if (schema != null){
				return schema.getString("$ref") == null && schema.getJsonObject("items") == null;
			}
			else {
				return jsonData.getJsonObject("items") == null;
			}
		}
		return true;
	}

	public static boolean isRequiredProperty(String key, JsonArray requiredJsonArray) {
		return requiredJsonArray == null || requiredJsonArray.getList().indexOf(key) != -1;
	}

	public static List<JsonArray> getJsonList(JsonArray arr) {
		return arr == null ? new ArrayList<JsonArray>() : arr.getList();
	}

	@SuppressWarnings("unchecked")
	public static String fetchOptionsTs(String className, String path, String method, JsonObject jsonData) {
		StringBuilder sb = new StringBuilder();
		JsonObject options = new JsonObject();

		String query = " + \"?";
		path += "/\"";
		JsonArray parameters = jsonData.getJsonArray("parameters");
		StringBuilder optHeader = new StringBuilder();
		if (method.equals("post") || method.equals("put")) {
			optHeader.append("\"Content-Type\":\"application/json\",");
		}

		Map<String, Integer> inTypeCounts = new HashMap<String, Integer>();
		inTypeCounts.put("query", 0);
		inTypeCounts.put("header", 0);
		inTypeCounts.put("path", 0);
		inTypeCounts.put("formData", 0);
		inTypeCounts.put("body", 0);

		Map<String, StringBuilder> inTypeAppendStrMap = new HashMap<String, StringBuilder>();
		inTypeAppendStrMap.put("formData", new StringBuilder());
		inTypeAppendStrMap.put("query", new StringBuilder());
		inTypeAppendStrMap.put("body", new StringBuilder());

		// inTypeAppendStrMap.get("header").append("h.append(\"Content-Type\",
		// \"application/json\");\r\n\t\t\t\t");

		if (parameters != null) {
			List<JsonObject> list = parameters.getList();
			for (int i = 0; i < list.size(); i++) {
				JsonObject param = list.get(i);
				String paramName = param.getString("name");
				String inType = param.getString("in");
				inTypeCounts.put(inType, inTypeCounts.get(inType) + 1);
				if (inType.equals("formData")) {
					inTypeAppendStrMap.get(inType)
							.append(String.format("f.append(\"%s\", %s);\r\n\t\t\t\t", paramName, paramName));
				} else if (inType.equals("query")) {
					query += String.format("%s=\" + %s + \"&", paramName, paramName);
				} else if (inType.equals("body")) {
					inTypeAppendStrMap.get(inType).append(String.format("%s, ", paramName));
				} else if (inType.equals("header")) {
					optHeader.append(String.format("\"%s\":%s,", paramName, paramName));
					// inTypeAppendStrMap.get(inType)
					// .append(String.format("h.append(\"%s\",
					// %s);\r\n\t\t\t\t", paramName, paramName));
				} else if (inType.equals("path")) {
					path += ".replace('{" + paramName + "}', " + paramName + ".toString())";
				}
			}
		}

		options.put("method", method.toUpperCase());

		// https://developer.mozilla.org/en-US/docs/Web/API/GlobalFetch/fetch
		// options.put("mode", "no-cors");
		// options.put("credentials", credentials);
		// options.put("cache", cache);
		// options.put("redirect", redirect);
		// options.put("referrer", referrer);
		// options.put("integrity", integrity);

		sb.append("\"/");
		sb.append(className);
		sb.append(path);
		if (query.length() > 1) {
			sb.append(query.substring(0, query.length() - 5));
		}
		sb.append(", ");

		if (optHeader.length() > 0)
			options.put("headers", "$HEADERS$");
		if (inTypeCounts.get("formData") != 0 || inTypeCounts.get("body") != 0) {
			options.put("body", "$BODY$");
		}
		String optStr = options.toString();
		if (optHeader.length() > 0) {
			String header = optHeader.toString();
			header = header.substring(0, header.length() - 1);
			optStr = optStr.replace("\"$HEADERS$\"", String.format("{%s}", header));
		}
		// optStr = optStr.replace("\"$HEADERS$\"",
		// String.format("(function () {\r\n\t\t\t\tvar h = new
		// Headers();\r\n\t\t\t\t%sreturn h;\r\n\t\t\t})()",
		// inTypeAppendStrMap.get("header").toString()));
		if (inTypeCounts.get("formData") != 0)
			optStr = optStr.replace("\"$BODY$\"",
					String.format(
							"(function () {\r\n\t\t\t\tvar f = new FormData();\r\n\t\t\t\t%sreturn f;\r\n\t\t\t})()",
							inTypeAppendStrMap.get("formData").toString()));
		else if (inTypeCounts.get("body") != 0) {
			String body = inTypeAppendStrMap.get("body").toString();
			body = body.substring(0, body.length() - 2);
			optStr = optStr.replace("\"$BODY$\"", String.format("JSON.stringify(%s)", body));
		}

		sb.append(optStr);
		return sb.toString();
	}

	// https://developer.mozilla.org/en-US/docs/Web/API/Response
	public static String fetchResponseType(String method) {
		return "json()";
	}

}
