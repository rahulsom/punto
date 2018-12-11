package com.github.rahulsom.punto.graal

//import com.github.rahulsom.punto.config.PuntoConfig
//import com.github.rahulsom.punto.config.Repository
import ch.qos.logback.classic.pattern.*
import ch.qos.logback.classic.pattern.color.HighlightingCompositeConverter
import ch.qos.logback.core.pattern.IdentityCompositeConverter
import ch.qos.logback.core.pattern.ReplacingCompositeConverter
import ch.qos.logback.core.pattern.color.*
import com.oracle.svm.core.annotate.AutomaticFeature
import org.graalvm.nativeimage.Feature
import org.graalvm.nativeimage.RuntimeReflection

@Suppress("unused")
@AutomaticFeature
class RuntimeReflectionRegistrationFeature : Feature {
    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
        listOf(
            DateConverter::class.java,
            MessageConverter::class.java,
            ThrowableProxyConverter::class.java,
            NopThrowableInformationConverter::class.java,
            ContextNameConverter::class.java,
            ReplacingCompositeConverter::class.java,
            BoldYellowCompositeConverter::class.java,
            BoldBlueCompositeConverter::class.java,
            CyanCompositeConverter::class.java,
            RedCompositeConverter::class.java,
            WhiteCompositeConverter::class.java,
            LoggerConverter::class.java,
            PropertyConverter::class.java,
            ExtendedThrowableProxyConverter::class.java,
            RootCauseFirstThrowableProxyConverter::class.java,
            MethodOfCallerConverter::class.java,
            LevelConverter::class.java,
            IdentityCompositeConverter::class.java,
            BoldWhiteCompositeConverter::class.java,
            MarkerConverter::class.java,
            BoldCyanCompositeConverter::class.java,
            BoldMagentaCompositeConverter::class.java,
            RelativeTimeConverter::class.java,
            MagentaCompositeConverter::class.java,
            ClassOfCallerConverter::class.java,
            LineOfCallerConverter::class.java,
            FileOfCallerConverter::class.java,
            BoldGreenCompositeConverter::class.java,
            LocalSequenceNumberConverter::class.java,
            YellowCompositeConverter::class.java,
            ExtendedThrowableProxyConverter::class.java,
            HighlightingCompositeConverter::class.java,
            GrayCompositeConverter::class.java,
            MDCConverter::class.java,
            ClassOfCallerConverter::class.java,
            BoldRedCompositeConverter::class.java,
            GreenCompositeConverter::class.java,
            BlackCompositeConverter::class.java,
            ThreadConverter::class.java,
            LineSeparatorConverter::class.java
        ).forEach {
            RuntimeReflection.register(it)
            RuntimeReflection.register(it.getDeclaredConstructor())
        }

//        val classes = listOf(
//            PuntoConfig::class.java,
//            Repository::class.java
//        )
//        classes.forEach {
//            RuntimeReflection.register(it)
//            RuntimeReflection.register(it.getDeclaredConstructor())
//            RuntimeReflection.register(*it.declaredMethods)
//        }
    }
}