package mm4d.jsonhelpers;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

//Credits to Atharva, M-T-A: http://stackoverflow.com/a/13120355
public class UHCSSuppressor implements JsonSerializer<Profile> {
	@Override
    public JsonElement serialize(Profile obj, Type type, JsonSerializationContext jsc) {
        Gson gson = new Gson();
        JsonObject jObj = (JsonObject)gson.toJsonTree(obj);   
        if(obj.useHopperCrashService == true){
            jObj.remove("useHopperCrashService");
        }
        return jObj;
    }
}
