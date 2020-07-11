package io.hotmoka.network.internal.models.function;

import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.network.internal.models.updates.ClassTagModel;

public class ClassTagMapper implements Mapper<ClassTag, ClassTagModel> {

    @Override
    public ClassTagModel map(ClassTag input) {
        ClassTagModel classTag = new ClassTagModel();
        classTag.setClassName(input.className);
        classTag.setJarHash(input.jar.getHash());
        return classTag;
    }
}
