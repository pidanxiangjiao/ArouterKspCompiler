package android.databinding.annotationprocessor;

import android.databinding.tool.CompilerArguments;
import android.databinding.tool.writer.JavaFileWriter;

import com.google.devtools.ksp.processing.Resolver;
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.xml.bind.JAXBException;

public abstract class ProcessingStep {
    private boolean mDone;
    public JavaFileWriter mJavaFileWriter;
    public Callback mCallback;

    protected JavaFileWriter getWriter() {
        return mJavaFileWriter;
    }

    public boolean runStep(RoundEnvironment roundEnvironment,
                            ProcessingEnvironment processingEnvironment,
                            CompilerArguments args) throws JAXBException {
        if (mDone) {
            return true;
        }
        mDone = onHandleStep(roundEnvironment, processingEnvironment, args);
        return mDone;
    }

    public boolean runKspStep(Resolver roundEnvironment,
                              SymbolProcessorEnvironment processingEnvironment,
                              CompilerArguments args) throws JAXBException {
        if (mDone) {
            return true;
        }
        mDone = onHandleKspStep(roundEnvironment, processingEnvironment, args);
        return mDone;
    }

    abstract public boolean onHandleKspStep(Resolver roundEnvironment,
                                            SymbolProcessorEnvironment processingEnvironment,
                                         CompilerArguments args);

    /**
     * Invoked in each annotation processing step.
     *
     * @return True if it is done and should never be invoked again.
     */
    abstract public boolean onHandleStep(RoundEnvironment roundEnvironment,
                                         ProcessingEnvironment processingEnvironment,
                                         CompilerArguments args) throws JAXBException;

    /**
     * Invoked when processing is done. A good place to generate the output if the
     * processor requires multiple steps.
     */
    abstract public void onProcessingOver(RoundEnvironment roundEnvironment,
                                          ProcessingEnvironment processingEnvironment,
                                          CompilerArguments args);
}
