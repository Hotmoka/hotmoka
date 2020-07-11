package io.hotmoka.network.internal.models.function;

import io.hotmoka.beans.updates.ClassTag;
import io.hotmoka.network.internal.models.ClassTagModel;

public class ClassTagMapper implements Mapper<ClassTag, ClassTagModel> {

    @Override
    public ClassTagModel map(ClassTag input) throws Exception {
        ClassTagModel classTag = new ClassTagModel();
        classTag.setClassName(input.className);
        classTag.setJarHash(input.jar.getHash());
        return classTag;
    }
}
