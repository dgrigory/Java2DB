package com.github.collinalpert.java2db.services;

import com.github.collinalpert.java2db.database.DBConnection;
import com.github.collinalpert.java2db.entities.BaseEntity;
import com.github.collinalpert.java2db.exceptions.IllegalEntityFieldAccessException;
import com.github.collinalpert.java2db.mappers.BaseMapper;
import com.github.collinalpert.java2db.mappers.Mapper;
import com.github.collinalpert.java2db.pagination.CacheablePaginationResult;
import com.github.collinalpert.java2db.pagination.PaginationResult;
import com.github.collinalpert.java2db.queries.OrderTypes;
import com.github.collinalpert.java2db.queries.Query;
import com.github.collinalpert.java2db.utilities.IoC;
import com.github.collinalpert.java2db.utilities.Utilities;
import com.github.collinalpert.lambda2sql.Lambda2Sql;
import com.github.collinalpert.lambda2sql.functions.SqlFunction;
import com.github.collinalpert.lambda2sql.functions.SqlPredicate;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Class that provides base functionality for all service classes. Every service class must extend this class.
 *
 * @author Collin Alpert
 */
public class BaseService<T extends BaseEntity> {

	private static final DateTimeFormatter dateTimeFormatter;
	private static final DateTimeFormatter dateFormatter;
	private static final DateTimeFormatter timeFormatter;

	static {
		dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
	}

	/**
	 * The generic type of this service.
	 */
	private final Class<T> type;

	/**
	 * Represents the table name of the entity this services corresponds to.
	 * It contains the table name with escaping backticks.
	 */
	private final String tableName;

	/**
	 * The mapper used for mapping database objects to Java entities in this service.
	 */
	private final Mapper<T> mapper;

	/**
	 * Constructor for the base class of all services. It is not possible to create instances of it.
	 */
	protected BaseService() {
		this.type = getGenericType();
		this.mapper = IoC.resolveMapperOrElse(this.type, new BaseMapper<>(this.type));
		this.tableName = String.format("`%s`", Utilities.getTableName(this.type));
	}

	//region Create

	/**
	 * Creates this Java entity on the database.
	 *
	 * @param instance The instance to create on the database.
	 * @return The id of the newly created record.
	 * @throws SQLException if the query cannot be executed due to database constraints
	 *                      i.e. non-existing default value for field or an incorrect data type or a foreign key constraint.
	 */
	public long create(T instance) throws SQLException {
		var insertQuery = createInsertHeader();
		var joiner = new StringJoiner(",", "(", ");");
		Utilities.getEntityFields(instance.getClass(), BaseEntity.class).forEach(field -> {
			field.setAccessible(true);
			joiner.add(getSQLValue(field, instance));
		});

		//For using the default database setting for the id.
		joiner.add("default");
		insertQuery.append(joiner.toString());
		try (var connection = new DBConnection()) {
			var id = connection.update(insertQuery.toString());
			Utilities.logf("%s successfully created!", this.type.getSimpleName());
			return id;
		}
	}

	/**
	 * Creates a variable amount of entities on the database.
	 *
	 * @param instances The instances to create.
	 * @throws SQLException if the query cannot be executed due to database constraints
	 *                      i.e. non-existing default value for field or an incorrect data type or a foreign key constraint.
	 * @see #createMultiple(List)
	 */
	@SafeVarargs
	public final void createMultiple(T... instances) throws SQLException {
		createMultiple(Arrays.asList(instances));
	}

	/**
	 * Creates multiple entities on the database.
	 * It is recommended to use this method instead of iterating over the list and
	 * calling a normal {@link #create(BaseEntity)} on each entity separately.
	 *
	 * @param instances The list of entities to create on the database.
	 * @throws SQLException if the query cannot be executed due to database constraints
	 *                      i.e. non-existing default value for field or an incorrect data type or a foreign key constraint.
	 */
	public void createMultiple(List<T> instances) throws SQLException {
		if (instances.isEmpty()) {
			return;
		}

		var insertQuery = createInsertHeader();
		var rows = new String[instances.size()];
		StringJoiner joiner;
		for (int i = 0, instancesSize = instances.size(); i < instancesSize; i++) {
			var entityFields = Utilities.getEntityFields(instances.get(i).getClass(), BaseEntity.class);
			joiner = new StringJoiner(", ", "(", ")");
			for (var entityField : entityFields) {
				entityField.setAccessible(true);
				joiner.add(getSQLValue(entityField, instances.get(i)));
			}

			//For using the default database setting for the id.
			joiner.add("default");
			rows[i] = joiner.toString();
		}

		insertQuery.append(String.join(", ", rows));
		try (var connection = new DBConnection()) {
			connection.update(insertQuery.toString());
			Utilities.logf("%s entities were successfully created.", this.type.getSimpleName());
		}
	}
	//endregion

	//region Read

	//region Count

	/**
	 * An overload of the {@link #count(SqlPredicate)} method. It will count all the rows that have an id, since this should be a reasonable criteria.
	 * Use with caution, as this call can take quite a while.
	 *
	 * @return The amount of rows in this table.
	 */
	public long count() {
		return count(x -> true);
	}

	/**
	 * Counts the rows that have an id (which should usually be every row) and match a certain condition. Note that COUNT operations usually are very slow.
	 *
	 * @param predicate The condition to test for.
	 * @return The number of rows matching the condition.
	 */
	public long count(SqlPredicate<T> predicate) {
		try (var connection = new DBConnection()) {
			try (var result = connection.execute(String.format("select count(id) from %s where %s;", this.tableName, Lambda2Sql.toSql(predicate, this.tableName)))) {
				if (result.next()) {
					return result.getLong("count(id)");
				}

				return 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(String.format("Could not get amount of rows in table %s for this predicate.", this.tableName));
		}
	}

	//endregion

	//region Any

	/**
	 * Checks if a table has at least one row.
	 *
	 * @return {@code True} if at least one row exists in the table, {@code false} if not.
	 */
	public boolean any() {
		return any(x -> true);
	}

	/**
	 * Checks if a value matching the condition exists in the table.
	 *
	 * @param predicate The condition to check for.
	 * @return {@code True} if the predicate matches one or more records, {@code false} if not.
	 */
	public boolean any(SqlPredicate<T> predicate) {
		try (var connection = new DBConnection()) {
			try (var result = connection.execute(String.format("select exists(select id from %s where %s limit 1) as result;", this.tableName, Lambda2Sql.toSql(predicate)))) {
				if (result.next()) {
					return result.getInt("result") == 1;
				}

				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(String.format("Could not check if a row matches this condition on table %s.", this.tableName));
		}
	}

	//endregion

	//region Query

	/**
	 * @return a {@link Query} object with which a DQL statement can be built, using operations like order by, limit etc.
	 * If you do not a plain {@link Query}, please consider using the
	 * {@link #getSingle(SqlPredicate)}, {@link #getMultiple(SqlPredicate)} or {@link #getAll()} methods.
	 */
	protected Query<T> createQuery() {
		return new Query<>(this.type, this.mapper);
	}

	/**
	 * Retrieves a single entity which matches the predicate.
	 * It is {@code protected} as it is only meant for use in methods of the respective service.
	 * This is to keep good programming practice and create descriptive methods for what kind of data you are getting.
	 *
	 * @param predicate The {@link SqlPredicate} to add constraints to a DQL query.
	 * @return An entity matching the result of the query.
	 */
	protected Optional<T> getSingle(SqlPredicate<T> predicate) {
		return createQuery().where(predicate).getFirst();
	}

	/**
	 * Retrieves list of entities which match the predicate.
	 * It is {@code protected} as it is only meant for use in methods of the respective service.
	 * This is to keep good programming practice and create descriptive methods for what kind of data you are getting.
	 *
	 * @param predicate The {@link SqlPredicate} to add constraints to a DQL statement.
	 * @return A list of entities matching the result of the query.
	 */
	protected Query<T> getMultiple(SqlPredicate<T> predicate) {
		return createQuery().where(predicate);
	}

	/**
	 * @param id The id of the desired entity.
	 * @return Gets an entity by its id.
	 */
	public Optional<T> getById(long id) {
		return getSingle(x -> x.getId() == id);
	}

	/**
	 * @return All entities in this table.
	 */
	public List<T> getAll() {
		return createQuery().toList();
	}

	/**
	 * Gets all values from the table but limits the result.
	 *
	 * @param limit The maximum of records to return.
	 * @return A list with the maximum size of the parameter specified.
	 */
	public List<T> getAll(int limit) {
		return createQuery().limit(limit).toList();
	}

	/**
	 * Gets all values from the table and orders them in an ascending order.
	 *
	 * @param orderBy The property to order by.
	 * @return A list of all records ordered by a specific property in an ascending order.
	 */
	public List<T> getAll(SqlFunction<T, ?> orderBy) {
		return getAll(orderBy, OrderTypes.ASCENDING);
	}

	/**
	 * Gets all values from the table and orders them in the specified order.
	 *
	 * @param orderBy     The property to order by.
	 * @param sortingType The order direction. Can be either ascending or descending.
	 * @return A list of all records ordered by a specific property in the specified order.
	 */
	public List<T> getAll(SqlFunction<T, ?> orderBy, OrderTypes sortingType) {
		return createQuery().orderBy(orderBy, sortingType).toList();
	}

	/**
	 * Gets all values from the table, orders them in an ascending order and limits the result.
	 *
	 * @param orderBy The property to order by.
	 * @param limit   The maximum records to return.
	 * @return A list with the maximum size of the parameter specified and in an ascending order.
	 */
	public List<T> getAll(SqlFunction<T, ?> orderBy, int limit) {
		return getAll(orderBy, OrderTypes.ASCENDING, limit);
	}

	/**
	 * Gets all values from the table, orders them in a specific order and limits the result.
	 *
	 * @param orderBy     The property to order by.
	 * @param sortingType The order direction. Can be either ascending or descending.
	 * @param limit       The maximum records to return.
	 * @return A list with the maximum size of the parameter specified and in an ascending order.
	 */
	public List<T> getAll(SqlFunction<T, ?> orderBy, OrderTypes sortingType, int limit) {
		return createQuery().orderBy(orderBy, sortingType).limit(limit).toList();
	}

	//endregion Query

	//region Pagination

	/**
	 * Creates a pagination structure that splits the entire table into multiple pages.
	 * Note that the query to the database for each page is only executed when that specific page is requested and
	 * <em>not</em> when this method is called.
	 *
	 * @param entriesPerPage The number of entries to be displayed on each page.
	 * @return A pagination which splits up an entire table into multiple pages.
	 */
	public PaginationResult<T> createPagination(int entriesPerPage) {
		return new PaginationResult<>(createPaginationQueries(entriesPerPage));
	}

	/**
	 * Creates a pagination structure that splits the result of a query into multiple pages.
	 * Note that the query to the database for each page is only executed when that specific page is requested and
	 * <em>not</em> when this method is called.
	 * The predicate to filter by.
	 *
	 * @param predicate      The predicate to filter by.
	 * @param entriesPerPage The number of entries to be displayed on each page.
	 * @return A pagination which splits the result of a condition into multiple pages.
	 */
	public PaginationResult<T> createPagination(SqlPredicate<T> predicate, int entriesPerPage) {
		return new PaginationResult<>(createPaginationQueries(predicate, entriesPerPage));
	}

	/**
	 * Creates a pagination structure that splits the entire table into multiple pages.
	 * Note that the query to the database for each page is only executed when that specific page is requested and
	 * <em>not</em> when this method is called.
	 * When pages are retrieved, they are stored in a cache, so they can be
	 * retrieved from there the next time they are requested. Note that this will work, until the specified {@code cacheExpiration}
	 * is up. After that, the next time a page is requested, it will be reloaded from the database.
	 *
	 * @param entriesPerPage  The number of entries to be displayed on each page.
	 * @param cacheExpiration The time a page is valid in the cache. If a page is requested and in the cache, but expired,
	 *                        it will be retrieved from the database again and re-stored in the cache.
	 * @return A pagination which displays a entire table
	 */
	public CacheablePaginationResult<T> createPagination(int entriesPerPage, Duration cacheExpiration) {
		return new CacheablePaginationResult<>(createPaginationQueries(entriesPerPage), cacheExpiration);
	}

	/**
	 * Creates a cached pagination structure that splits the result of a query into multiple pages.
	 * Note that the query to the database for each page is only executed when that specific page is requested and
	 * <em>not</em> when this method is called.
	 * When pages are retrieved, they are stored in a cache, so they can be
	 * retrieved from there the next time they are requested. Note that this will work, until the specified {@code cacheExpiration}
	 * is up. After that, the next time a page is requested, it will be reloaded from the database.
	 *
	 * @param predicate       The predicate to filter by.
	 * @param entriesPerPage  The number of entries to be displayed on each page.
	 * @param cacheExpiration The time a page is valid in the cache. If a page is requested and in the cache, but expired,
	 *                        it will be retrieved from the database again and re-stored in the cache.
	 * @return A pagination which allows the developer to retrieve specific pages from the result.
	 */
	public CacheablePaginationResult<T> createPagination(SqlPredicate<T> predicate, int entriesPerPage, Duration cacheExpiration) {
		return new CacheablePaginationResult<>(createPaginationQueries(predicate, entriesPerPage), cacheExpiration);
	}

	//endregion

	//endregion

	//region Update

	/**
	 * Updates this entity's row on the database.
	 *
	 * @param instance The instance to update on the database.
	 * @throws SQLException if the query cannot be executed due to database constraints
	 *                      i.e. non-existing default value for field or an incorrect data type.
	 */
	public void update(T instance) throws SQLException {
		var updateQuery = new StringBuilder("update ").append(this.tableName).append(" set ");
		var fieldJoiner = new StringJoiner(", ");
		Utilities.getEntityFields(instance.getClass(), BaseEntity.class).forEach(field -> {
			field.setAccessible(true);
			try {
				fieldJoiner.add(String.format("`%s` = %s", field.getName(), convertObject(field.get(instance))));
			} catch (IllegalAccessException e) {
				System.err.printf("Error getting value for field %s from type %s\n", field.getName(), this.type.getSimpleName());
			}
		});

		updateQuery.append(fieldJoiner.toString()).append(" where id = ").append(instance.getId());
		try (var connection = new DBConnection()) {
			connection.update(updateQuery.toString());
			Utilities.logf("%s with id %d was successfully updated.", this.type.getSimpleName(), instance.getId());
		}
	}

	/**
	 * Updates a specific column for a record in a table.
	 *
	 * @param entityId The id of the record.
	 * @param column   The column to update.
	 * @param newValue The new value of the column.
	 * @param <R>      The data type of the column. It must be the same as the data type of the new value.
	 * @throws SQLException if the query cannot be executed due to database constraints
	 *                      i.e. non-existing default value for field or an incorrect data type.
	 */
	public <R> void update(long entityId, SqlFunction<T, R> column, R newValue) throws SQLException {
		var query = new StringBuilder("update ");
		query.append(this.tableName).append(" set ").append(Lambda2Sql.toSql(column, this.tableName))
				.append(" = ").append(convertObject(newValue)).append(" where ").append(this.tableName)
				.append(".id = ").append(entityId).append(';');
		try (var connection = new DBConnection()) {
			connection.update(query.toString());
			Utilities.logf("%s with id %d was successfully updated.", this.type.getSimpleName(), entityId);
		}
	}
	//endregion

	//region Delete

	/**
	 * Deletes the corresponding row on the database.
	 *
	 * @param instance The instance to delete on the database.
	 * @throws SQLException for example because of a foreign key constraint.
	 */
	public void delete(T instance) throws SQLException {
		delete(instance.getId());
	}

	/**
	 * Deletes a row by an id.
	 *
	 * @param id The row with this id to delete.
	 * @throws SQLException for example because of a foreign key constraint.
	 */
	public void delete(long id) throws SQLException {
		try (var connection = new DBConnection()) {
			connection.update(String.format("delete from %s where %s.id = ?;", this.tableName, this.tableName), id);
			Utilities.logf("%s with id %s successfully deleted!", this.type.getSimpleName(), id);
		}
	}

	/**
	 * Deletes rows based on a condition.
	 *
	 * @param predicate The condition to delete by.
	 * @throws SQLException in case the condition cannot be applied or if a foreign key constraint fails.
	 */
	public void delete(SqlPredicate<T> predicate) throws SQLException {
		try (var connection = new DBConnection()) {
			connection.update(String.format("delete from %s where %s;", this.tableName, Lambda2Sql.toSql(predicate, this.tableName)));
			Utilities.logf("%s successfully deleted!", this.type.getSimpleName());
		}
	}

	/**
	 * Truncates the corresponding table on the database.
	 *
	 * @throws SQLException for example if a foreign key references a row in this table.
	 */
	public void truncateTable() throws SQLException {
		try (var connection = new DBConnection()) {
			connection.update(String.format("truncate table %s;", this.tableName));
			Utilities.logf("Table %s successfully truncated.", this.tableName);
		}
	}

	//endregion

	/**
	 * Gets the generic type of this class.
	 *
	 * @return The entity class used as a generic type for this BaseService.
	 */
	@SuppressWarnings("unchecked")
	private Class<T> getGenericType() {
		return ((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
	}

	/**
	 * Converts a Java object to its SQL representation.
	 *
	 * @param value The value to convert.
	 * @return The SQL version of a Java value.
	 */
	private String convertObject(Object value) {
		if (value == null) {
			return "null";
		}

		if (value instanceof String) {
			return "'" + value + "'";
		}

		if (value instanceof Boolean) {
			var bool = (boolean) value;
			return bool ? "1" : "0";
		}

		if (value instanceof LocalDateTime) {
			var dateTime = (LocalDateTime) value;
			return "'" + dateTimeFormatter.format(dateTime) + "'";
		}

		if (value instanceof LocalDate) {
			var date = (LocalDate) value;
			return "'" + dateFormatter.format(date) + "'";
		}

		if (value instanceof LocalTime) {
			var time = (LocalTime) value;
			return "'" + timeFormatter.format(time) + "'";
		}

		return value.toString();
	}

	/**
	 * Will create a {@link StringBuilder} containing the beginning of a DML INSERT statement.
	 *
	 * @return An INSERT statement up to the VALUES keyword.
	 */
	private StringBuilder createInsertHeader() {
		var insertQuery = new StringBuilder("insert into ").append(this.tableName).append(" (");
		var databaseFields = Utilities.getEntityFields(this.type).stream().map(field -> String.format("`%s`", field.getName())).collect(Collectors.joining(", "));
		insertQuery.append(databaseFields).append(") values ");
		return insertQuery;
	}

	/**
	 * Returns a field value from ab entity in its SQL equivalent.
	 *
	 * @param entityField    The value's field.
	 * @param entityInstance The entity containing the value
	 * @return A {@code String} representing the SQL value of the entity field.
	 */
	private String getSQLValue(Field entityField, T entityInstance) {
		try {
			return convertObject(entityField.get(entityInstance));
		} catch (IllegalAccessException e) {
			throw new IllegalEntityFieldAccessException(entityField.getName(), this.type.getSimpleName(), e.getMessage());
		}
	}

	/**
	 * An overload of the {@link #createPaginationQueries(SqlPredicate, int)} method.
	 * It creates queries for getting all the values from a table.
	 *
	 * @param entriesPerPage The number of entries to be displayed on each page.
	 * @return A list of queries containing the definitions for getting specific pages.
	 */
	private List<Query<T>> createPaginationQueries(int entriesPerPage) {
		return createPaginationQueries(x -> true, entriesPerPage);
	}

	/**
	 * Creates queries that define the queries for specific pages. Note that this does not execute the queries yet.
	 *
	 * @param predicate      A filter to narrow down the queries.
	 * @param entriesPerPage The number of entries to be displayed on each page.
	 * @return A list of queries containing the definitions for getting specific pages.
	 */
	private List<Query<T>> createPaginationQueries(SqlPredicate<T> predicate, int entriesPerPage) {
		var count = this.count(predicate);
		var numberOfPages = Math.ceil(count / (double) entriesPerPage);
		var queries = new ArrayList<Query<T>>((int) numberOfPages);
		for (int i = 0; i < numberOfPages; i++) {
			var offset = i * entriesPerPage;
			//TODO Refactor the limit-offset-combo for performance in bigger data sets.
			queries.add(getMultiple(predicate).limit(entriesPerPage, offset));
		}

		return queries;
	}
}