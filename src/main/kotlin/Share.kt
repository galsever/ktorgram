package org.srino

import io.ktor.http.HttpMethod
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.RoutingHandler
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

class ShareRoute(
    val path: String,
    val method: HttpMethod,
    val input: KClass<*>,
    val output: KClass<*>,
    val params: List<String>
)

private fun ShareRoute.toFunctionName(): String {
    val methodPrefix = method.value.lowercase()
    val segments = path
        .split('/')
        .filter { it.isNotBlank() }
        .map { seg -> seg.replaceFirstChar { it.uppercaseChar() } }

    val suffix = if (segments.isEmpty()) "Root" else segments.joinToString("")
    return methodPrefix + suffix
}

class Share {

    private val classes: MutableSet<KClass<*>> = mutableSetOf()
    private val routes: MutableList<ShareRoute> = mutableListOf()

    fun addRoute(path: String, method: HttpMethod, input: KClass<*>, output: KClass<*>) {
        val (cleanPath, params) = extractPathAndParams(path)

        classes.add(input)
        classes.add(output)
        routes.add(ShareRoute(cleanPath, method, input, output, params))
    }

    fun routes(): String {
        val sb = StringBuilder()

        // import all collected types except Unit
        val typeNames = classes
            .mapNotNull { it.simpleName }
            .filterNot { it == "Unit" }
            .sorted()

        if (typeNames.isNotEmpty()) {
            sb.appendLine("import { ${typeNames.joinToString(", ")} } from \"./types\"")
        }
        sb.appendLine("import { request } from \"./request\"")
        sb.appendLine()

        routes.forEach { route ->
            val inputName = route.input.simpleName ?: "any"
            val outputName = route.output.simpleName ?: "any"

            val functionName = route.toFunctionName()

            val hasBody = route.input != Unit::class
            val bodyVarName = if (hasBody) {
                inputName.replaceFirstChar { it.lowercaseChar() }
            } else {
                null
            }

            val params = mutableListOf<String>()
            if (hasBody) {
                params += "$bodyVarName: $inputName"
            }
            route.params.forEach { paramName ->
                params += "$paramName: string"
            }
            val paramsSignature = params.joinToString(", ")

            val bodyArg = if (hasBody) {
                "JSON.stringify($bodyVarName)"
            } else {
                "\"\""
            }

            val paramsArray = if (route.params.isEmpty()) {
                "[]"
            } else {
                "[${route.params.joinToString(", ")}]"
            }

            val pathForRequest = if (route.params.isEmpty()) {
                route.path // e.g. "/users"
            } else {
                if (route.path.endsWith("/")) route.path else route.path + "/"
            }

            sb.appendLine("export async function $functionName($paramsSignature): Promise<$outputName> {")
            sb.appendLine("    const res = await request(")
            sb.appendLine("        \"$pathForRequest\",")
            sb.appendLine("        \"${route.method.value}\",")
            sb.appendLine("        $bodyArg,")
            sb.appendLine("        $paramsArray")
            sb.appendLine("    );")
            sb.appendLine("    return res as $outputName;")
            sb.appendLine("}")
            sb.appendLine()
        }

        return sb.toString()
    }

    fun definitions(): String {
        val sb = StringBuilder()

        classes.forEach {
            sb.appendLine(generateTsInterface(it))
            sb.appendLine()
        }

        return sb.toString()
    }

    private fun generateTsInterface(clazz: KClass<*>): String {
        val name = clazz.simpleName ?: error("Class $clazz has no name")
        val props = clazz.memberProperties

        val sb = StringBuilder()
        sb.appendLine("export interface $name {")
        props.forEach { prop ->
            val propName = prop.name
            val tsType = prop.returnType
            sb.appendLine("    $propName: ${tsType.tsType()};")
        }
        sb.appendLine("}")
        return sb.toString()
    }

    private fun KType.tsType(): String {
        val classifier = classifier as? KClass<*> ?: error("Classifier is not a class")
        val base = when (classifier) {
            Byte::class,
            Short::class,
            Int::class,
            Long::class,
            Float::class,
            Double::class -> "number"

            Boolean::class -> "boolean"

            String::class,
            Char::class -> "string"

            List::class,
            MutableList::class,
            Set::class,
            MutableSet::class -> {
                val argType = arguments.firstOrNull()?.type ?: return "any[]"
                "${argType.tsType()}[]"
            }

            Array::class -> {
                val argType = arguments.firstOrNull()?.type ?: return "any[]"
                "${argType.tsType()}[]"
            }

            Map::class,
            MutableMap::class -> {
                val keyType = arguments.getOrNull(0)?.type
                val valueType = arguments.getOrNull(1)?.type

                val tsKey = keyType?.tsType() ?: "string"
                val tsValue = valueType?.tsType() ?: "any"

                val keyForIndex = when (tsKey) {
                    "string", "number" -> tsKey
                    else -> "string"
                }

                "{ [key: $keyForIndex]: $tsValue }"
            }

            else -> classifier.simpleName ?: "any"
        }
        return if (isMarkedNullable) "$base | null" else base
    }
    private fun extractPathAndParams(path: String): Pair<String, List<String>> {
        val segments = path
            .split('/')
            .filter { it.isNotBlank() }

        val staticSegments = mutableListOf<String>()
        val params = mutableListOf<String>()

        for (segment in segments) {
            if (segment.startsWith("{") && segment.endsWith("}")) {
                val inner = segment.substring(1, segment.length - 1)
                // support patterns like {id}, {id?}, {id:[0-9]+}
                val namePart = inner.takeWhile { it != ':' }
                val name = namePart.trimEnd('?')
                params += name
            } else {
                staticSegments += segment
            }
        }

        val cleanPath = if (staticSegments.isEmpty()) {
            "/"
        } else {
            "/" + staticSegments.joinToString("/")
        }

        return cleanPath to params
    }
}

fun Share.registerRoute(path: String, method: HttpMethod, input: KClass<*>, output: KClass<*>) {
    addRoute(path, method, input, output)
}

inline fun <reified Input, reified Output> Route.post(path: String, noinline body: RoutingHandler) {
    post(path, body)
    share.registerRoute(path, HttpMethod.Post, Input::class, Output::class)
}