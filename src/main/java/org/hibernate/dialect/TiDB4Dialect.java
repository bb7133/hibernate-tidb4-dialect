/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorTiDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import java.time.Duration;

public class TiDB4Dialect extends MySQL57Dialect {

	private static final String QUERY_SEQUENCES_STRING =
			"SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = database()";

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		return false;
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize)
			throws MappingException {
		final String sequenceString = getCreateSequenceString( sequenceName ) + " start with " + initialValue + " increment by " + incrementSize;
		// TiDB has defaults for min and max value that don't play well with settings then sign( increment ) != sign( initialValue )
		if ( incrementSize > 0 && initialValue < 0 ) {
			return sequenceString + " minvalue " + initialValue;
		}
		else if ( incrementSize < 0 && initialValue > 0 ) {
			return sequenceString + " maxvalue " + initialValue;
		}
		else {
			return sequenceString;
		}
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName );
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval(" + sequenceName + ")";
	}

	@Override
	public String getQuerySequencesString() {
		return QUERY_SEQUENCES_STRING;
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorTiDBDatabaseImpl.INSTANCE;
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( timeout == LockOptions.NO_WAIT ) {
			return getForUpdateNowaitString();
		}

		if ( timeout > 0 ) {
			return getForUpdateString() + " wait " + getLockWaitTimeoutInSeconds( timeout );
		}

		return getForUpdateString();
	}

	@Override
	public String getForUpdateNowaitString() {
		return getForUpdateString() + " nowait";
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString( aliases ) + " nowait";
	}

	private static long getLockWaitTimeoutInSeconds(int timeoutInMilliseconds) {
		Duration duration = Duration.ofMillis( timeoutInMilliseconds );
		return duration.getSeconds();
	}

}
