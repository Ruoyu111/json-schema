package org.everit.json.schema.loader;

import org.everit.json.schema.SchemaException;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author erosb
 */
public class JSONTraverserTest {

    public static final String FINISH = "finish";

    private static class DummyJSONVisitor implements JSONVisitor<String> {

        @Override public String visitBoolean(boolean value, LoadingState ls) {
            return "boolean";
        }

        @Override public String visitArray(List<JSONTraverser> value, LoadingState ls) {
            return "array";
        }

        @Override public String visitString(String value, LoadingState ls) {
            return "string";
        }

        @Override public String visitInteger(Integer value, LoadingState ls) {
            return "integer";
        }

        @Override public String visitObject(Map<String, JSONTraverser> obj, LoadingState ls) {
            return "object";
        }

        @Override public String visitNull(LoadingState ls) {
            return "null";
        }

        @Override public String finishedVisiting(LoadingState ls) {
            return null;
        }
    }

    private DummyJSONVisitor dummyVisitor = new DummyJSONVisitor();

    private final LoadingState emptyLs = new LoadingState(SchemaLoader.builder()
            .rootSchemaJson(new JSONObject())
            .schemaJson(new JSONObject()));

    @Rule
    public ExpectedException exc  = ExpectedException.none();

    @Test
    public void testBoolean() {
        JSONVisitor<String> visitor = spy(dummyVisitor);
        JSONTraverser subject = new JSONTraverser(true, emptyLs);
        String actual = subject.accept(visitor);
        verify(visitor).visitBoolean(eq(true), notNull(LoadingState.class));
        verify(visitor).finishedVisiting(emptyLs);
        assertEquals("boolean", actual);
    }

    @Test
    public void boolArray() {
        dummyVisitor = new DummyJSONVisitor();
        JSONVisitor<String> visitor = spy(dummyVisitor);
        JSONArray array = new JSONArray("[true]");
        JSONTraverser subject = new JSONTraverser(array, emptyLs);
        String actual = subject.accept(visitor);
        verify(visitor).visitArray(argThat(listOf(new JSONTraverser(true, emptyLs))), notNull(LoadingState.class));
        verify(visitor).finishedVisiting(emptyLs);
        assertEquals("array", actual);
    }

    @Test
    public void testString() {
        JSONVisitor<String> visitor = spy(dummyVisitor);
        JSONTraverser subject = new JSONTraverser("string", emptyLs);
        String actual = subject.accept(visitor);
        verify(visitor).visitString(eq("string"), notNull(LoadingState.class));
        verify(visitor).finishedVisiting(emptyLs);
        assertEquals("string", actual);
    }

    @Test
    public void testJSONNullHandling() {
        JSONVisitor<String> visitor = spy(dummyVisitor);
        JSONTraverser subject = new JSONTraverser(JSONObject.NULL, emptyLs);
        String actual = subject.accept(visitor);
        verify(visitor).visitNull(emptyLs);
        verify(visitor).finishedVisiting(emptyLs);
        assertEquals("null", actual);
    }

    @Test
    public void testNullReferenceHandling() {
        JSONVisitor<String> visitor = spy(dummyVisitor);
        JSONTraverser subject = new JSONTraverser(null, emptyLs);
        String actual = subject.accept(visitor);
        verify(visitor).visitNull(emptyLs);
        verify(visitor).finishedVisiting(emptyLs);
        assertEquals("null", actual);
    }

    @Test
    public void testObject() {
        JSONVisitor<String> visitor = spy(dummyVisitor);
        JSONTraverser subject = new JSONTraverser(new JSONObject("{\"a\":true}"), emptyLs);
        String actual = subject.accept(visitor);
        HashMap<String, JSONTraverser> expected = new HashMap<>();
        expected.put("a", new JSONTraverser(true, emptyLs));
        verify(visitor).visitObject(eq(expected), notNull(LoadingState.class));
        verify(visitor).finishedVisiting(emptyLs);
        assertEquals("object", actual);
    }

    @Test
    public void emptyObj() {
        JSONVisitor<String> visitor = spy(dummyVisitor);
        JSONTraverser subject = new JSONTraverser(new JSONObject("{}"), emptyLs);
        String  actual = subject.accept(visitor);
        verify(visitor).visitObject(eq(emptyMap()), notNull(LoadingState.class));
        verify(visitor).finishedVisiting(emptyLs);
        assertEquals("object", actual);
    }

    private Matcher<List<JSONTraverser>> listOf(JSONTraverser... expected) {
        return new TypeSafeMatcher<List<JSONTraverser>>() {

            @Override protected boolean matchesSafely(List<JSONTraverser> item) {
                return new ArrayList<>(item).equals(new ArrayList<>(asList(expected)));
            }

            @Override public void describeTo(Description description) {

            }
        };
    }

    @Test
    public void ptrChangeOnArray() {
        JSONVisitor subject = new BaseJSONVisitor<Void>() {

            @Override public Void visitBoolean(boolean value, LoadingState ls) {
                if (value) {
                    assertEquals(asList("0"), ls.pointerToCurrentObj);
                } else {
                    assertEquals(asList("1"), ls.pointerToCurrentObj);
                }
                return null;
            }

        };
        new JSONTraverser(new JSONArray("[true,false]"), emptyLs).accept(subject);
    }

    @Test
    public void ptrChangeOnObject() {
        JSONVisitor<String> subject = new BaseJSONVisitor<String>() {

            @Override public String visitBoolean(boolean value, LoadingState ls) {
                if (value) {
                    assertEquals(asList("a"), ls.pointerToCurrentObj);
                } else {
                    assertEquals(asList("b"), ls.pointerToCurrentObj);
                }
                return null;
            }

        };
        new JSONTraverser(new JSONObject("{\"a\":true,\"b\":false}"), emptyLs).accept(subject);
    }

    @Test
    public void finisherOverridesRetval() {
        JSONVisitor<String> visitor = new DummyJSONVisitor() {

            @Override public String finishedVisiting(LoadingState ls) {
                return FINISH;
            }
        };
        String actual = new JSONTraverser(true, emptyLs).accept(visitor);
        assertEquals(FINISH, actual);
    }

    @Test
    public void requireString() {
        exc.expect(SchemaException.class);
        JSONVisitor.requireString(new JSONTraverser(true, emptyLs));
    }

}
