package io.quarkiverse.langchain4j.azure.openai.deployment;

import static io.quarkiverse.langchain4j.deployment.LangChain4jDotNames.CHAT_MODEL;
import static io.quarkiverse.langchain4j.deployment.LangChain4jDotNames.EMBEDDING_MODEL;
import static io.quarkiverse.langchain4j.deployment.LangChain4jDotNames.IMAGE_MODEL;
import static io.quarkiverse.langchain4j.deployment.LangChain4jDotNames.STREAMING_CHAT_MODEL;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkiverse.langchain4j.ModelName;
import io.quarkiverse.langchain4j.azure.openai.runtime.AzureOpenAiRecorder;
import io.quarkiverse.langchain4j.deployment.DotNames;
import io.quarkiverse.langchain4j.deployment.items.ChatModelProviderCandidateBuildItem;
import io.quarkiverse.langchain4j.deployment.items.EmbeddingModelProviderCandidateBuildItem;
import io.quarkiverse.langchain4j.deployment.items.ImageModelProviderCandidateBuildItem;
import io.quarkiverse.langchain4j.deployment.items.ModerationModelProviderCandidateBuildItem;
import io.quarkiverse.langchain4j.deployment.items.SelectedChatModelProviderBuildItem;
import io.quarkiverse.langchain4j.deployment.items.SelectedEmbeddingModelCandidateBuildItem;
import io.quarkiverse.langchain4j.deployment.items.SelectedImageModelProviderBuildItem;
import io.quarkiverse.langchain4j.runtime.NamedConfigUtil;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;

public class AzureOpenAiProcessor {

    private static final String FEATURE = "langchain4j-azure-openai";
    private static final String PROVIDER = "azure-openai";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public void providerCandidates(BuildProducer<ChatModelProviderCandidateBuildItem> chatProducer,
            BuildProducer<EmbeddingModelProviderCandidateBuildItem> embeddingProducer,
            BuildProducer<ModerationModelProviderCandidateBuildItem> moderationProducer,
            BuildProducer<ImageModelProviderCandidateBuildItem> imageProducer,
            LangChain4jAzureOpenAiBuildConfig config) {
        if (config.chatModel().enabled().isEmpty() || config.chatModel().enabled().get()) {
            chatProducer.produce(new ChatModelProviderCandidateBuildItem(PROVIDER));
        }
        if (config.embeddingModel().enabled().isEmpty() || config.embeddingModel().enabled().get()) {
            embeddingProducer.produce(new EmbeddingModelProviderCandidateBuildItem(PROVIDER));
        }
        if (config.imageModel().enabled().isEmpty() || config.imageModel().enabled().get()) {
            imageProducer.produce(new ImageModelProviderCandidateBuildItem(PROVIDER));
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void generateBeans(AzureOpenAiRecorder recorder,
            List<SelectedChatModelProviderBuildItem> selectedChatItem,
            List<SelectedEmbeddingModelCandidateBuildItem> selectedEmbedding,
            List<SelectedImageModelProviderBuildItem> selectedImage,
            BuildProducer<SyntheticBeanBuildItem> beanProducer) {
        for (var selected : selectedChatItem) {
            if (PROVIDER.equals(selected.getProvider())) {
                String configName = selected.getConfigName();

                var chatModel = recorder.chatModel(configName);
                var chatBuilder = SyntheticBeanBuildItem
                        .configure(CHAT_MODEL)
                        .setRuntimeInit()
                        .defaultBean()
                        .scope(ApplicationScoped.class)
                        .addInjectionPoint(ParameterizedType.create(DotNames.CDI_INSTANCE,
                                new Type[] { ClassType.create(DotNames.CHAT_MODEL_LISTENER) }, null))
                        .addInjectionPoint(ParameterizedType.create(DotNames.CDI_INSTANCE,
                                new Type[] { ClassType.create(DotNames.MODEL_AUTH_PROVIDER) }, null))
                        .createWith(chatModel);
                addQualifierIfNecessary(chatBuilder, configName);
                beanProducer.produce(chatBuilder.done());

                var streamingChatModel = recorder.streamingChatModel(configName);
                var streamingBuilder = SyntheticBeanBuildItem
                        .configure(STREAMING_CHAT_MODEL)
                        .setRuntimeInit()
                        .defaultBean()
                        .scope(ApplicationScoped.class)
                        .addInjectionPoint(ParameterizedType.create(DotNames.CDI_INSTANCE,
                                new Type[] { ClassType.create(DotNames.CHAT_MODEL_LISTENER) }, null))
                        .addInjectionPoint(ParameterizedType.create(DotNames.CDI_INSTANCE,
                                new Type[] { ClassType.create(DotNames.MODEL_AUTH_PROVIDER) }, null))
                        .createWith(streamingChatModel);
                addQualifierIfNecessary(streamingBuilder, configName);
                beanProducer.produce(streamingBuilder.done());
            }
        }

        for (var selected : selectedEmbedding) {
            if (PROVIDER.equals(selected.getProvider())) {
                String configName = selected.getConfigName();

                var embeddingModel = recorder.embeddingModel(configName);
                var builder = SyntheticBeanBuildItem
                        .configure(EMBEDDING_MODEL)
                        .setRuntimeInit()
                        .unremovable()
                        .defaultBean()
                        .scope(ApplicationScoped.class)
                        .addInjectionPoint(ParameterizedType.create(DotNames.CDI_INSTANCE,
                                new Type[] { ClassType.create(DotNames.MODEL_AUTH_PROVIDER) }, null))
                        .createWith(embeddingModel);
                addQualifierIfNecessary(builder, configName);
                beanProducer.produce(builder.done());
            }
        }

        for (var selected : selectedImage) {
            if (PROVIDER.equals(selected.getProvider())) {
                String configName = selected.getConfigName();

                var imageModel = recorder.imageModel(configName);
                var builder = SyntheticBeanBuildItem
                        .configure(IMAGE_MODEL)
                        .setRuntimeInit()
                        .defaultBean()
                        .scope(ApplicationScoped.class)
                        .addInjectionPoint(ParameterizedType.create(DotNames.CDI_INSTANCE,
                                new Type[] { ClassType.create(DotNames.MODEL_AUTH_PROVIDER) }, null))
                        .createWith(imageModel);
                addQualifierIfNecessary(builder, configName);
                beanProducer.produce(builder.done());
            }
        }
    }

    private void addQualifierIfNecessary(SyntheticBeanBuildItem.ExtendedBeanConfigurator builder, String configName) {
        if (!NamedConfigUtil.isDefault(configName)) {
            builder.addQualifier(AnnotationInstance.builder(ModelName.class).add("value", configName).build());
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void cleanUp(AzureOpenAiRecorder recorder, ShutdownContextBuildItem shutdown) {
        recorder.cleanUp(shutdown);
    }
}
