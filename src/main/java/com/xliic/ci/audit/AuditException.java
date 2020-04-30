/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.ci.audit;

public class AuditException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6795526460296139587L;
	private Throwable error;

	public AuditException(String message) {
		super(message);
	}

	public AuditException(String message, Throwable error) {
		super(message);
		this.error = error;
	}

	@Override
	public String getMessage() {
		String message = super.getMessage();
		if (this.error != null) {
			return message + ": " + this.error.getMessage();
		}
		return message;
	}

	public Throwable getError() {
		return error;
	}
}
