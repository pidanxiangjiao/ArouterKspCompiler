package android.databinding.annotationprocessor;

import android.databinding.tool.CompilerChef;
import android.databinding.tool.store.GenClassInfoLog;

import java.util.List;

public interface Callback {
    void onChefReady(CompilerChef chef, GenClassInfoLog classInfoLog);
    void onBrWriterReady(BindableBag.BRMapping brWithValues, List<String> brPackages);
}
