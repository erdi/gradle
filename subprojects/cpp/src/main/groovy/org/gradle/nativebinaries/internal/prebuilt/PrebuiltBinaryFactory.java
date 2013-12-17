/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.nativebinaries.internal.prebuilt;

import org.gradle.internal.reflect.Instantiator;
import org.gradle.language.base.internal.DefaultBinaryNamingScheme;
import org.gradle.nativebinaries.BuildType;
import org.gradle.nativebinaries.Flavor;
import org.gradle.nativebinaries.NativeComponent;
import org.gradle.nativebinaries.PrebuiltLibrary;
import org.gradle.nativebinaries.internal.NativeComponentInternal;
import org.gradle.nativebinaries.platform.Platform;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class PrebuiltBinaryFactory {
    private final Instantiator instantiator;
    private final Set<Platform> allPlatforms = new LinkedHashSet<Platform>();
    private final Set<BuildType> allBuildTypes = new LinkedHashSet<BuildType>();
    private final Set<Flavor> allFlavors = new LinkedHashSet<Flavor>();

    public PrebuiltBinaryFactory(Instantiator instantiator,
                                 Collection<? extends Platform> allPlatforms, Collection<? extends BuildType> allBuildTypes, Collection<? extends Flavor> allFlavors) {
        this.instantiator = instantiator;
        this.allPlatforms.addAll(allPlatforms);
        this.allBuildTypes.addAll(allBuildTypes);
        this.allFlavors.addAll(allFlavors);
    }

    public void initialise(PrebuiltLibrary library) {
        createNativeBinaries((NativeComponentInternal) library);
    }

    public void createNativeBinaries(NativeComponentInternal component) {
         for (Platform platform : component.choosePlatforms(allPlatforms)) {
             for (BuildType buildType : component.chooseBuildTypes(allBuildTypes)) {
                 for (Flavor flavor : component.chooseFlavors(allFlavors)) {
                     createNativeBinaries(component, platform, buildType, flavor);
                 }
             }
         }
    }

    public void createNativeBinaries(NativeComponent component, Platform platform, BuildType buildType, Flavor flavor) {
        createNativeBinary(PrebuiltApiLibraryBinary.class, component, platform, buildType, flavor);
        createNativeBinary(PrebuiltSharedLibraryBinary.class, component, platform, buildType, flavor);
        createNativeBinary(PrebuiltStaticLibraryBinary.class, component, platform, buildType, flavor);
    }

    public <T extends PrebuiltLibraryBinary> void createNativeBinary(Class<T> type, NativeComponent component, Platform platform, BuildType buildType, Flavor flavor) {
        DefaultBinaryNamingScheme namingScheme = createNamingScheme(component, platform, buildType, flavor);
        T nativeBinary = instantiator.newInstance(type, namingScheme, component, buildType, platform, flavor);
        component.getBinaries().add(nativeBinary);
    }

    // TODO:DAZ Duplication
    private DefaultBinaryNamingScheme createNamingScheme(NativeComponent component, Platform platform, BuildType buildType, Flavor flavor) {
        DefaultBinaryNamingScheme namingScheme = new DefaultBinaryNamingScheme(component.getName());
        if (usePlatformDimension(component)) {
            namingScheme = namingScheme.withVariantDimension(platform.getName());
        }
        if (useBuildTypeDimension(component)) {
            namingScheme = namingScheme.withVariantDimension(buildType.getName());
        }
        if (useFlavorDimension(component)) {
            namingScheme = namingScheme.withVariantDimension(flavor.getName());
        }
        return namingScheme;
    }

    private boolean usePlatformDimension(NativeComponent component) {
        return ((NativeComponentInternal) component).choosePlatforms(allPlatforms).size() > 1;
    }

    private boolean useBuildTypeDimension(NativeComponent component) {
        return ((NativeComponentInternal) component).chooseBuildTypes(allBuildTypes).size() > 1;
    }

    private boolean useFlavorDimension(NativeComponent component) {
        return ((NativeComponentInternal) component).chooseFlavors(allFlavors).size() > 1;
    }
}
