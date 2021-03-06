package com.github.collinalpert.java2db.database;

import com.github.collinalpert.java2db.annotations.ForeignKeyEntity;
import com.github.collinalpert.java2db.modules.TableModule;

import java.lang.reflect.Field;

/**
 * Describes a column and its table name so they can be referenced together.
 *
 * @author Collin Alpert
 */
public class TableColumnReference {

	private static final TableModule tableModule;

	static {
		tableModule = new TableModule();
	}

	/**
	 * The table name of this reference.
	 */
	private final String tableName;

	/**
	 * The column name of this reference.
	 */
	private final Field column;

	/**
	 * The alias for this reference, if one exists.
	 * If it doesn't, the table name will be used.
	 */
	private final String alias;

	/**
	 * If this reference is a foreign key, this field displays the foreign key that references this table.
	 */
	private final String referenceColumn;

	public TableColumnReference(String tableName, Field column, String alias, String referenceColumn) {
		this.tableName = tableName;
		this.column = column;
		this.alias = alias;
		this.referenceColumn = referenceColumn;
	}

	public Field getColumn() {
		return column;
	}

	public String getAlias() {
		return alias;
	}

	public String getReference() {
		return referenceColumn.isEmpty() ? tableName : referenceColumn;
	}

	public String getSQLNotation() {
		return String.format("`%s`.`%s`", getIdentifier(), tableModule.getColumnName(column));
	}

	public String getAliasNotation() {
		return getIdentifier() + "_" + tableModule.getColumnName(column);
	}

	public boolean isForeignKey() {
		return column.getAnnotation(ForeignKeyEntity.class) != null;
	}

	public String getIdentifier() {
		return alias.isEmpty() ? tableName : alias;
	}

	@Override
	public String toString() {
		return getSQLNotation();
	}
}
