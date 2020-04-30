/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit.model.api;

public class Maybe<T> implements ErrorContainer {
    private T result;
    private ErrorMessage error;

    public Maybe(T result) {
        this.result = result;
    }

    public Maybe(ErrorMessage error) {
        this.error = error;
    }

    public boolean isOk() {
        return this.error == null;
    }

    public boolean isError() {
        return this.error != null;
    }

    public ErrorMessage getError() {
        return error;
    }

    public T getResult() {
        return result;
    }
}
