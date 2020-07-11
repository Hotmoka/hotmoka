package io.hotmoka.network.internal.models.function;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.network.internal.models.transactions.TransactionReferenceModel;

public class TransactionReferenceMapper implements Mapper<TransactionReference, TransactionReferenceModel> {

    @Override
    public TransactionReferenceModel map(TransactionReference input) throws Exception {
        TransactionReferenceModel transactionReferenceModel = new TransactionReferenceModel();
        transactionReferenceModel.setHash(input.getHash());
        return transactionReferenceModel;
    }
}
