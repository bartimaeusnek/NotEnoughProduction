package com.bigbass.nep.recipes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.github.axet.wget.WGet;

public class RecipeManager {
	
	private static RecipeManager instance;
	
	/**
	 * <p>Table of recipes.</p>
	 * 
	 * <p>Please take care when adding recipes! The key is NOT the source name from the JSON data, but rather the
	 * name of the crafting/process in which the recipe goes. So for a Gregtech recipe in the Compressor,
	 * the key would be "Compressor", and all compressor recipes (in a list) would be the key's value.</p>
	 */
	public Hashtable<String, List<IRecipe>> recipes;
	
	/**
	 * Contains a list of sources as defined by the JSON data. Each source's value is a list that contains
	 * the keys from {@link RecipeManager#recipes} that originate from the source.
	 */
	public Hashtable<String, List<String>> recipeSources;
	
	private RecipeManager(){
		recipes = new Hashtable<String, List<IRecipe>>();
		recipeSources = new Hashtable<String, List<String>>();
	}
	
	public static RecipeManager getInst(){
		if(instance == null){
			instance = new RecipeManager();
		}
		
		return instance;
	}
	
	/**
	 * <p>Flushes any currently loaded recipes and attempts to load the specified version.</p>
	 * 
	 * <p>If the version is not found locally, it will attempt to download it. If the version does
	 * not exist on the remote server, a RecipeError will be thrown.</p>
	 * 
	 * @param version
	 * @return
	 */
	public RecipeError loadRecipes(String version){
		if(version == null || version.trim().isEmpty()){
			return new RecipeError("emptyVersion", "The version provided was either null or empty.");
		}
		
		version = version.trim().replace(".json", "");
		FileHandle handle = Gdx.files.local("cache/" + version + ".json");
		
		// File Checking and Retrieval \\
		
		if(!handle.exists()){
			try {
				URL url = new URL("http://libgdxjam.com/recex/" + version + ".json");
				File target = handle.file();
				
				WGet w = new WGet(url, target);
				
				w.download(); // blocking! attempts to download file from the url to the target File.
			} catch (MalformedURLException e) {
				return new RecipeError("malformed", e.getMessage());
			} catch (RuntimeException e) {
				e.printStackTrace();
				return new RecipeError("runtime", e.getMessage());
			}
		}
		
		if(!handle.exists()){
			return new RecipeError("fileNotFound", "After attempting to download the version, the file still cannot be found.");
		}
		
		// Recipe Parsing \\
		
		JsonObject root;
		
		try {
			JsonReader reader = Json.createReader(new FileReader(handle.file()));
			
			root = reader.readObject();
			
			reader.close();
		} catch (FileNotFoundException e) {
			return new RecipeError("fileNotFound", "After attempting to download the version, the file still cannot be found.");
		} catch (JsonException | IllegalStateException e){
			return new RecipeError("parsing", "Error occured during parsing: " + e.getMessage());
		}
		
		for(JsonObject source : root.getJsonArray("sources").getValuesAs(JsonObject.class)){
			final String sourceType = source.getString("type", "unknown");
			
			
			if(sourceType.equalsIgnoreCase("gregtech")){ //*************** GREGTECH ***************\\
				JsonArray machines = source.getJsonArray("machines");
				ArrayList<String> machineNames = new ArrayList<String>();
				
				for(JsonObject machine : machines.getValuesAs(JsonObject.class)){
					final List<IRecipe> machineRecipes = new ArrayList<IRecipe>();
					final String machineName = machine.getString("n", "unknown");
					
					for(JsonObject jsonRecipe : machine.getJsonArray("recs").getValuesAs(JsonObject.class)){
						if(!jsonRecipe.isEmpty()){
							final GregtechRecipe recipe = new GregtechRecipe();
							recipe.machineName = machineName;
							recipe.enabled = jsonRecipe.getBoolean("en", false);
							recipe.duration = jsonRecipe.getInt("dur", 0);
							recipe.eut = jsonRecipe.getInt("eut", 0);
							
							// item inputs
							for(JsonObject jsonItem : jsonRecipe.getJsonArray("iI").getValuesAs(JsonObject.class)){
								final Item item = parseItem(jsonItem);
								if(item != null){
									recipe.itemInputs.add(item);
								}
							}
							// item outputs
							for(JsonObject jsonItem : jsonRecipe.getJsonArray("iO").getValuesAs(JsonObject.class)){
								final Item item = parseItem(jsonItem);
								if(item != null){
									recipe.itemOutputs.add(item);
								}
							}
							// fluid inputs
							for(JsonObject jsonFluid : jsonRecipe.getJsonArray("fI").getValuesAs(JsonObject.class)){
								final Fluid fluid = parseFluid(jsonFluid);
								if(fluid != null){
									recipe.fluidInputs.add(fluid);
								}
							}
							// fluid outputs
							for(JsonObject jsonFluid : jsonRecipe.getJsonArray("fO").getValuesAs(JsonObject.class)){
								final Fluid fluid = parseFluid(jsonFluid);
								if(fluid != null){
									recipe.fluidOutputs.add(fluid);
								}
							}
							
							machineRecipes.add(recipe);
						}
					}
					
					if(!machineRecipes.isEmpty()){
						recipes.put(machineName, machineRecipes);
						machineNames.add(machineName);
					}
				}
				recipeSources.put(sourceType, machineNames);
				
			} else if(sourceType.equalsIgnoreCase("shapeless")){ //*************** SHAPELESS ***************\\
				final List<String> craftingType = new ArrayList<String>(1);
				craftingType.add("Shapeless Crafting");
				recipeSources.put(sourceType, craftingType);
				
				final List<IRecipe> shapelessRecipes = new ArrayList<IRecipe>();
				for(JsonObject jsonRecipe : source.getJsonArray("recipes").getValuesAs(JsonObject.class)){
					if(!jsonRecipe.isEmpty()){
						final ShapelessRecipe recipe = new ShapelessRecipe();
						
						for(JsonObject jsonItem : jsonRecipe.getJsonArray("iI").getValuesAs(JsonObject.class)){
							final Item item = parseItem(jsonItem);
							if(item != null){
								recipe.itemInputs.add(item);
							}
						}
						
						final Item output = parseItem(jsonRecipe.getJsonObject("o"));
						if(output != null){
							recipe.itemOutput = output;
						}
						
						shapelessRecipes.add(recipe);
					}
				}
				
				recipes.put("Shapeless Crafting", shapelessRecipes);
			} else if(sourceType.equalsIgnoreCase("shaped")){ //*************** SHAPED ***************\\
				final List<String> craftingType = new ArrayList<String>(1);
				craftingType.add("Shaped Crafting");
				recipeSources.put(sourceType, craftingType);
				
				final List<IRecipe> shapedRecipes = new ArrayList<IRecipe>();
				for(JsonObject jsonRecipe : source.getJsonArray("recipes").getValuesAs(JsonObject.class)){
					if(!jsonRecipe.isEmpty()){
						final ShapedRecipe recipe = new ShapedRecipe();
						
						for(JsonValue jsonItem : jsonRecipe.getJsonArray("iI").getValuesAs(JsonValue.class)){
							if(jsonItem != JsonValue.NULL && jsonItem instanceof JsonObject){
								final Item item = parseItem((JsonObject) jsonItem);
								if(item != null){
									recipe.itemInputs.add(item);
								}
							}
						}
						
						final Item output = parseItem(jsonRecipe.getJsonObject("o"));
						if(output != null){
							recipe.itemOutput = output;
						}
						
						shapedRecipes.add(recipe);
					}
				}
				
				recipes.put("Shaped Crafting", shapedRecipes);
			} //TODO oredicted recipes
		}
		
		return null; // intended; returning null means no errors occured
	}
	
	public Item parseItem(JsonObject json){
		if(json == null || json.isEmpty()){
			return null;
		}
		
		Item item = null;
		
		if(json.containsKey("cfg")){
			item = new ItemProgrammedCircuit();
			((ItemProgrammedCircuit) item).configNum = json.getInt("cfg");
		} else {
			item = new Item();
		}
		
		item.amount = json.getInt("a", 0);
		item.unlocalizedName = json.getString("uN", "");
		item.localizedName = json.getString("lN", "");
		
		if((item.unlocalizedName == null || item.unlocalizedName.isEmpty()) && (item.localizedName == null || item.localizedName.isEmpty())){
			// if item is not identifiable, return null
			return null;
		}
		
		return item;
	}
	
	public Fluid parseFluid(JsonObject json){
		if(json == null || json.isEmpty()){
			return null;
		}
		
		Fluid fluid = new Fluid();
		
		fluid.amount = json.getInt("a", 0);
		fluid.unlocalizedName = json.getString("uN", "");
		fluid.localizedName = json.getString("lN", "");
		
		if((fluid.unlocalizedName == null || fluid.unlocalizedName.isEmpty()) && (fluid.localizedName == null || fluid.localizedName.isEmpty())){
			// if item is not identifiable, return null
			return null;
		}
		
		return fluid;
	}
	
	/**
	 * Attempts to locate a particular recipe by comparing the provided hashCode with the hashCode of every loaded recipe.
	 * 
	 * This function may cause some lag depending on how fast it finds the recipe.
	 * 
	 * @param hashCode of the recipe
	 * @return the found recipe, or null
	 */
	public IRecipe findRecipe(int hashCode){
		if(hashCode == -1){
			return null;
		}
		
		if(recipes != null){
			for(String key : recipes.keySet()){
				for(IRecipe rec : recipes.get(key)){
					if(rec.hashCode() == hashCode){
						return rec;
					}
				}
			}
		}
		
		return null;
	}
	
	
	public class RecipeError {
		
		private final String code;
		private final String description;
		
		public RecipeError(String code, String description){
			this.code = code;
			this.description = description;
		}
		
		public String getCode(){
			return code;
		}
		
		public String getDescription(){
			return description;
		}
		
		@Override
		public String toString(){
			return code;
		}
	}
}
