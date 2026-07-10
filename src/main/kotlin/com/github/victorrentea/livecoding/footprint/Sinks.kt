package com.github.victorrentea.livecoding.footprint

/**
 * "Whole-object" channels that read every field without any visible getter call. If the target
 * reaches one of these, a thin DTO would silently break at runtime, so the analyzer reports
 * WHOLE_OBJECT instead of a misleadingly short field list.
 *
 * These are common Java serialization / deep-copy / scripting entry points. Project-specific
 * whole-object method names (custom XML/JSON marshalling like `toXML`/`fromXML`) are included as
 * frequently-seen heuristics; a Settings page to edit them is the natural next step.
 */
object Sinks {

    /** Instance methods on the target that touch every field (serialization / marshalling). */
    val WHOLE_OBJECT_METHODS = setOf(
        "writeObject", "writeExternal", "readObject", "readExternal", // java (de)serialization
        "clone",                                                      // deep copy
        "toXML", "toShortXML", "toMobileXML", "fromXML", "fromResultSet", // common marshalling
    )

    /**
     * Method names that hand an arbitrary object to a serializer or a script engine. Matched
     * together with a qualifier-type hint in [SINK_QUALIFIER_TYPES] to avoid false hits on
     * unrelated methods named put()/clone()/eval()/writeValue().
     */
    val SINK_METHODS = setOf(
        "writeObject", "writeUnshared",       // ObjectOutputStream -> whole graph
        "clone", "serialize", "deepClone",    // SerializationUtils / deep-copy utils
        "writeValue", "writeValueAsString",   // Jackson ObjectMapper -> whole object
        "toJson", "toXML",                    // Gson / XStream -> whole object
        "put", "eval", "setBindings",         // javax.script.ScriptEngine / Bindings
    )

    /** Simple type-name fragments that confirm a [SINK_METHODS] call is really a sink. */
    val SINK_QUALIFIER_TYPES = setOf(
        "ObjectOutputStream", "SerializationUtils",
        "ObjectMapper", "Gson", "XStream",
        "ScriptEngine", "Bindings", "ScriptContext", "Invocable",
    )
}
