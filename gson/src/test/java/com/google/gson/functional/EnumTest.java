/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson.functional;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.common.MoreAsserts;
import com.google.gson.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.common.truth.Truth.assertThat;

/**
 * Functional tests for Java 5.0 enums.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class EnumTest {

  private Gson gson;

  @Before
  public void setUp() throws Exception {
    gson = new Gson();
  }

  @Test
  public void testTopLevelEnumSerialization() {
    String result = gson.toJson(MyEnum.VALUE1);
    assertThat(result).isEqualTo('"' + MyEnum.VALUE1.toString() + '"');
  }

  @Test
  public void testTopLevelEnumDeserialization() {
    MyEnum result = gson.deserializeFromJson('"' + MyEnum.VALUE1.toString() + '"', MyEnum.class);
    assertThat(result).isEqualTo(MyEnum.VALUE1);
  }

  @Test
  public void testCollectionOfEnumsSerialization() {
    Type type = new TypeToken<Collection<MyEnum>>() {}.getType();
    Collection<MyEnum> target = new ArrayList<>();
    target.add(MyEnum.VALUE1);
    target.add(MyEnum.VALUE2);
    String expectedJson = "[\"VALUE1\",\"VALUE2\"]";
    String actualJson = gson.toJson(target);
    assertThat(actualJson).isEqualTo(expectedJson);
    actualJson = gson.toJson(target, type);
    assertThat(actualJson).isEqualTo(expectedJson);
  }

  @Test
  public void testCollectionOfEnumsDeserialization() {
    Type type = new TypeToken<Collection<MyEnum>>() {}.getType();
    String json = "[\"VALUE1\",\"VALUE2\"]";
    Collection<MyEnum> target = gson.deserializeFromJson(json, type);
    MoreAsserts.assertContains(target, MyEnum.VALUE1);
    MoreAsserts.assertContains(target, MyEnum.VALUE2);
  }

  @Test
  public void testClassWithEnumFieldSerialization() {
    ClassWithEnumFields target = new ClassWithEnumFields();
    assertThat(gson.toJson(target)).isEqualTo(target.getExpectedJson());
  }

  @Test
  public void testClassWithEnumFieldDeserialization() {
    String json = "{value1:'VALUE1',value2:'VALUE2'}";
    ClassWithEnumFields target = gson.deserializeFromJson(json, ClassWithEnumFields.class);
    assertThat(target.value1).isEqualTo(MyEnum.VALUE1);
    assertThat(target.value2).isEqualTo(MyEnum.VALUE2);
  }

  private static enum MyEnum {
    VALUE1,
    VALUE2
  }

  private static class ClassWithEnumFields {
    private final MyEnum value1 = MyEnum.VALUE1;
    private final MyEnum value2 = MyEnum.VALUE2;

    public String getExpectedJson() {
      return "{\"value1\":\"" + value1 + "\",\"value2\":\"" + value2 + "\"}";
    }
  }

  /** Test for issue 226. */
  @Test
  @SuppressWarnings("GetClassOnEnum")
  public void testEnumSubclass() {
    assertThat(Roshambo.ROCK.getClass()).isNotEqualTo(Roshambo.class);
    assertThat(gson.toJson(Roshambo.ROCK)).isEqualTo("\"ROCK\"");
    assertThat(gson.toJson(EnumSet.allOf(Roshambo.class)))
        .isEqualTo("[\"ROCK\",\"PAPER\",\"SCISSORS\"]");
    assertThat(gson.deserializeFromJson("\"ROCK\"", Roshambo.class)).isEqualTo(Roshambo.ROCK);
    Set<Roshambo> deserialized =
        gson.deserializeFromJson("[\"ROCK\",\"PAPER\",\"SCISSORS\"]", new TypeToken<>() {});
    assertThat(deserialized).isEqualTo(EnumSet.allOf(Roshambo.class));

    // A bit contrived, but should also work if explicitly deserializing using anonymous enum
    // subclass
    assertThat(gson.deserializeFromJson("\"ROCK\"", Roshambo.ROCK.getClass())).isEqualTo(Roshambo.ROCK);
  }

  @Test
  @SuppressWarnings("GetClassOnEnum")
  public void testEnumSubclassWithRegisteredTypeAdapter() {
    gson =
        new GsonBuilder()
            .registerTypeHierarchyAdapter(Roshambo.class, new MyEnumTypeAdapter())
            .create();
    assertThat(Roshambo.ROCK.getClass()).isNotEqualTo(Roshambo.class);
    assertThat(gson.toJson(Roshambo.ROCK)).isEqualTo("\"123ROCK\"");
    assertThat(gson.toJson(EnumSet.allOf(Roshambo.class)))
        .isEqualTo("[\"123ROCK\",\"123PAPER\",\"123SCISSORS\"]");
    assertThat(gson.deserializeFromJson("\"123ROCK\"", Roshambo.class)).isEqualTo(Roshambo.ROCK);
    Set<Roshambo> deserialized =
        gson.deserializeFromJson("[\"123ROCK\",\"123PAPER\",\"123SCISSORS\"]", new TypeToken<>() {});
    assertThat(deserialized).isEqualTo(EnumSet.allOf(Roshambo.class));
  }

  @Test
  public void testEnumSubclassAsParameterizedType() {
    Collection<Roshambo> list = new ArrayList<>();
    list.add(Roshambo.ROCK);
    list.add(Roshambo.PAPER);

    String json = gson.toJson(list);
    assertThat(json).isEqualTo("[\"ROCK\",\"PAPER\"]");

    Type collectionType = new TypeToken<Collection<Roshambo>>() {}.getType();
    Collection<Roshambo> actualJsonList = gson.deserializeFromJson(json, collectionType);
    MoreAsserts.assertContains(actualJsonList, Roshambo.ROCK);
    MoreAsserts.assertContains(actualJsonList, Roshambo.PAPER);
  }

  @Test
  public void testEnumCaseMapping() {
    assertThat(gson.deserializeFromJson("\"boy\"", Gender.class)).isEqualTo(Gender.MALE);
    assertThat(gson.toJson(Gender.MALE, Gender.class)).isEqualTo("\"boy\"");
  }

  @Test
  public void testEnumSet() {
    EnumSet<Roshambo> foo = EnumSet.of(Roshambo.ROCK, Roshambo.PAPER);
    String json = gson.toJson(foo);
    assertThat(json).isEqualTo("[\"ROCK\",\"PAPER\"]");

    Type type = new TypeToken<EnumSet<Roshambo>>() {}.getType();
    EnumSet<Roshambo> bar = gson.deserializeFromJson(json, type);
    assertThat(bar).containsExactly(Roshambo.ROCK, Roshambo.PAPER).inOrder();
    assertThat(bar).doesNotContain(Roshambo.SCISSORS);
  }

  @Test
  public void testEnumMap() {
    EnumMap<MyEnum, String> map = new EnumMap<>(MyEnum.class);
    map.put(MyEnum.VALUE1, "test");
    String json = gson.toJson(map);
    assertThat(json).isEqualTo("{\"VALUE1\":\"test\"}");

    Type type = new TypeToken<EnumMap<MyEnum, String>>() {}.getType();
    EnumMap<?, ?> actualMap = gson.deserializeFromJson("{\"VALUE1\":\"test\"}", type);
    Map<?, ?> expectedMap = Collections.singletonMap(MyEnum.VALUE1, "test");
    assertThat(actualMap).isEqualTo(expectedMap);
  }

  private enum Roshambo {
    ROCK {
      @Override
      Roshambo defeats() {
        return SCISSORS;
      }
    },
    PAPER {
      @Override
      Roshambo defeats() {
        return ROCK;
      }
    },
    SCISSORS {
      @Override
      Roshambo defeats() {
        return PAPER;
      }
    };

    @SuppressWarnings("unused")
    abstract Roshambo defeats();
  }

  private static class MyEnumTypeAdapter
      implements JsonSerializer<Roshambo>, JsonDeserializer<Roshambo> {
    @Override
    public JsonElement serialize(Roshambo src, Type typeOfSrc, JsonSerializationContext context) {
      return new JsonPrimitive("123" + src.name());
    }

    @Override
    public Roshambo deserialize(JsonElement json, Type classOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return Roshambo.valueOf(json.getAsString().substring(3));
    }
  }

  private enum Gender {
    @SerializedName("boy")
    MALE,

    @SerializedName("girl")
    FEMALE
  }

  @Test
  public void testEnumClassWithFields() {
    assertThat(gson.toJson(Color.RED)).isEqualTo("\"RED\"");
    assertThat(gson.deserializeFromJson("RED", Color.class).value).isEqualTo("red");
    assertThat(gson.deserializeFromJson("BLUE", Color.class).index).isEqualTo(2);
  }

  private enum Color {
    RED("red", 1),
    BLUE("blue", 2),
    GREEN("green", 3);
    final String value;
    final int index;

    private Color(String value, int index) {
      this.value = value;
      this.index = index;
    }
  }

  @Test
  public void testEnumToStringRead() {
    // Should still be able to read constant name
    assertThat(gson.deserializeFromJson("\"A\"", CustomToString.class)).isEqualTo(CustomToString.A);
    // Should be able to read toString() value
    assertThat(gson.deserializeFromJson("\"test\"", CustomToString.class)).isEqualTo(CustomToString.A);

    assertThat(gson.deserializeFromJson("\"other\"", CustomToString.class)).isNull();
  }

  private enum CustomToString {
    A;

    @Override
    public String toString() {
      return "test";
    }
  }

  /** Test that enum constant names have higher precedence than {@code toString()} result. */
  @Test
  public void testEnumToStringReadInterchanged() {
    assertThat(gson.deserializeFromJson("\"A\"", InterchangedToString.class))
        .isEqualTo(InterchangedToString.A);
    assertThat(gson.deserializeFromJson("\"B\"", InterchangedToString.class))
        .isEqualTo(InterchangedToString.B);
  }

  private enum InterchangedToString {
    A("B"),
    B("A");

    private final String toString;

    InterchangedToString(String toString) {
      this.toString = toString;
    }

    @Override
    public String toString() {
      return toString;
    }
  }

  /** This test case focuses on the deserialization of a generic data structure using 
   * TypeToken to accurately maintain type information. */
  @Test
  public void testDeserializationOfGenericStructureWithTypeToken() {
	  String json = "[{\"key\":\"value1\"}, {\"key\":\"value2\"}]";
	  Gson gson = new Gson();
	  Type type = new TypeToken<List<Map<String, String>>>() {}.getType();
	  List<Map<String, String>> list = gson.deserializeFromJson(json, type);

	  assertThat(list.size()).isEqualTo(2);
	  assertThat(list.get(0).get("key")).isEqualTo("value1");
	  assertThat(list.get(1).get("key")).isEqualTo("value2");
  }


  /** This test ensures that custom serializers and deserializers for 
   * enums work as expected, preserving the ability to encode additional information or apply custom logic. */
  enum TestEnum {
	  VALUE_ONE, VALUE_TWO
  }

  @Test
  public void testCustomSerializationAndDeserializationOfEnums() {
	  GsonBuilder builder = new GsonBuilder();
	  builder.registerTypeAdapter(TestEnum.class, new JsonSerializer<TestEnum>() {
		  @Override
		  public JsonElement serialize(TestEnum src, Type typeOfSrc, JsonSerializationContext context) {
			  return new JsonPrimitive(src.toString().toLowerCase());
		  }
	  }).registerTypeAdapter(TestEnum.class, new JsonDeserializer<TestEnum>() {
		  @Override
		  public TestEnum deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			  return TestEnum.valueOf(json.getAsString().toUpperCase());
		  }
	  });
	  Gson gson = builder.create();

	  String json = gson.toJson(TestEnum.VALUE_ONE);
	  assertThat(json).isEqualTo("\"value_one\"");

	  TestEnum enumValue = gson.deserializeFromJson("\"value_one\"", TestEnum.class);
	  assertThat(enumValue).isEqualTo(TestEnum.VALUE_ONE);
  }

  /** This test verifies the custom date formats during serialization 
   * and deserialization, ensuring that GSON can correctly parse and 
   * format dates according to specified patterns. */
  @Test
  public void testHandlingDateFormats() throws ParseException {
	  String pattern = "yyyy-MM-dd";
	  Gson gson = new GsonBuilder().setDateFormat(pattern).create();
	  SimpleDateFormat format = new SimpleDateFormat(pattern);
	  Date date = format.parse("2024-01-01");

	  String json = gson.toJson(date);
	  assertThat(json).isEqualTo("\"2024-01-01\"");

	  Date deserializedDate = gson.deserializeFromJson("\"2024-01-01\"", Date.class);
	  assertThat(format.format(deserializedDate)).isEqualTo("2024-01-01");
  }


}
