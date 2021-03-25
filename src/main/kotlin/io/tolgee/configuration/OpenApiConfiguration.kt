package io.tolgee.configuration

import io.swagger.v3.oas.models.*
import io.tolgee.openapi_fixtures.InternalIgnorePaths
import io.tolgee.security.api_key_auth.AccessWithApiKey
import org.reflections.Reflections
import org.springdoc.core.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerMethod
import io.swagger.v3.oas.models.info.Info


@Configuration
open class OpenApiConfiguration() {

    @Bean
    open fun springShopOpenAPI(): OpenAPI? {
        return OpenAPI()
                .info(Info().title("Tolgee API ")
                        .description("Tolgee Server API reference")
                        .version("v1.0")
                )
                .externalDocs(ExternalDocumentation()
                        .description("Toolgee")
                        .url("https://toolkit.tolgee.io"))
    }

    @Bean
    open fun internalOpenApi(): GroupedOpenApi? {
        val paths = arrayOf("/api/**")
        val apiPaths = this.apiKeyPaths

        return GroupedOpenApi.builder().group("Internal - for Tolgee Web application")
                .pathsToMatch(*paths)
                .addOpenApiCustomiser { openApi ->
                    val newPaths = Paths()
                    openApi.paths.forEach { pathEntry ->
                        val operations = ArrayList<Operation>()
                        val newPathItem = PathItem()
                        val oldPathItem = pathEntry.value
                        oldPathItem.readOperations().forEach { operation ->
                            val isParameterConsumed = operation?.parameters?.any { it.name == "repositoryId" } == true
                            val pathContainsRepositoryIdParam = pathEntry.key.contains("{repositoryId}")
                            val paramIsConsumedAndInPath = isParameterConsumed && pathContainsRepositoryIdParam
                            val parameterIsMissingAtAll = !pathContainsRepositoryIdParam && !isParameterConsumed

                            if (paramIsConsumedAndInPath || parameterIsMissingAtAll) {
                                operations.add(operation)
                            }

                            operation.parameters?.removeIf { it.name == "ak" && it.`in` == "query" }
                        }

                        operations.forEach { operation ->
                            newPathItem.operation(oldPathItem.getHttpMethod(operation), operation)
                        }

                        if (operations.isNotEmpty()) {
                            newPaths.addPathItem(pathEntry.key, newPathItem)
                        }
                    }
                    openApi.paths = newPaths
                }
                .pathsToExclude(*apiPaths.toTypedArray(), "/api/repository/{repositoryId}/sources/**")
                .build()
    }

    @Bean
    open fun apiKeyOpenApi(): GroupedOpenApi? {
        val operationHandlers = HashMap<Operation, HandlerMethod>()

        return GroupedOpenApi.builder().group("Accessible with API key")
                .pathsToMatch("/api/**")
                .addOpenApiCustomiser { openApi ->
                    val newPaths = Paths()
                    openApi.paths.forEach { pathEntry ->
                        val operations = ArrayList<Operation>()
                        val newPathItem = PathItem()
                        val oldPathItem = pathEntry.value
                        oldPathItem.readOperations().forEach { operation ->
                            val annotation = operationHandlers[operation]?.getMethodAnnotation(AccessWithApiKey::class.java)
                            if (annotation != null) {
                                val containsRepositoryIdParam = pathEntry.key.contains("{repositoryId}")
                                if (!pathEntry.key.startsWith("/api/repository/{repositoryId}")) {
                                    if (!containsRepositoryIdParam) {
                                        operation.parameters.removeIf { it.name == "repositoryId" }
                                    }
                                    operations.add(operation)
                                }
                            }
                        }

                        operations.forEach { operation ->
                            newPathItem.operation(oldPathItem.getHttpMethod(operation), operation)
                        }

                        if (operations.isNotEmpty()) {
                            newPaths.addPathItem(pathEntry.key, newPathItem)
                        }
                    }
                    openApi.paths = newPaths

                    val usedTags = newPaths.flatMap { it.value.readOperations() }.flatMap { it.tags }
                    openApi.tags.removeIf { !usedTags.contains(it.name) }


                    val usedSchemas = mutableListOf<String>()
                    newPaths.forEach {
                        it.value.readOperations().forEach {
                            it.requestBody?.content?.values?.forEach {
                                it?.schema?.`$ref`?.let {
                                    usedSchemas.add(it.replace("#/components/schemas/", ""))
                                }
                            }
                            it.responses?.values?.forEach {
                                it.content?.values?.forEach {
                                    it.schema?.`$ref`?.let {
                                        usedSchemas.add(it.replace("#/components/schemas/", ""))
                                    }
                                }
                            }
                        }
                    }

                    openApi.components.schemas.entries.removeIf { !usedSchemas.contains(it.key) }

                }
                .addOperationCustomizer { operation: Operation, handlerMethod: HandlerMethod ->
                    operationHandlers.put(operation, handlerMethod)
                    operation
                }.build()
    }

    private fun PathItem.getHttpMethod(operation: Operation): PathItem.HttpMethod? {
        return when (operation) {
            this.get -> PathItem.HttpMethod.GET
            this.delete -> PathItem.HttpMethod.DELETE
            this.head -> PathItem.HttpMethod.HEAD
            this.post -> PathItem.HttpMethod.POST
            this.patch -> PathItem.HttpMethod.PATCH
            this.put -> PathItem.HttpMethod.PUT
            this.options -> PathItem.HttpMethod.OPTIONS
            this.trace -> PathItem.HttpMethod.TRACE
            else -> null
        }
    }

    private val apiKeyPaths: List<String> by lazy {
        val reflections = Reflections("io.tolgee.controllers")

        reflections.getTypesAnnotatedWith(InternalIgnorePaths::class.java)
                .flatMap { clazz ->
                    val methodPaths = clazz.declaredMethods.asSequence()
                            .filter { it.isAnnotationPresent(InternalIgnorePaths::class.java) }
                            .flatMap { it.getAnnotation(InternalIgnorePaths::class.java).value.asSequence() }
                            .toList()
                    val classPaths = clazz.getAnnotation(InternalIgnorePaths::class.java).value
                    val withMethods = classPaths
                            .flatMap { classPath -> methodPaths.map { classPath + it } }

                    if (withMethods.isNotEmpty()) withMethods else classPaths.toList()
                }
    }
}

