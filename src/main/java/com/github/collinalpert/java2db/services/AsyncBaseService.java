package com.github.collinalpert.java2db.services;

import com.github.collinalpert.java2db.entities.BaseEntity;
import com.github.collinalpert.java2db.queries.OrderTypes;
import com.github.collinalpert.java2db.utilities.CallbackUtils;
import com.github.collinalpert.lambda2sql.functions.SqlFunction;
import com.github.collinalpert.lambda2sql.functions.SqlPredicate;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.github.collinalpert.java2db.utilities.Utilities.runnableHandling;
import static com.github.collinalpert.java2db.utilities.Utilities.supplierHandling;

/**
 * This class extends the functionality of a service class by adding support for asynchronous CRUD operations.
 * Inherit from this class instead of the {@link BaseService} if you want your services to be able to do stuff asynchronously.
 *
 * @author Collin Alpert
 * @see BaseService
 */
public class AsyncBaseService<T extends BaseEntity> extends BaseService<T> {

	//region Create

	/**
	 * The asynchronous version of the {@link #create(BaseEntity)} method without custom exception handling and without a callback.
	 *
	 * @param instance The instance to create on the database asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #create(BaseEntity)
	 */
	public CompletableFuture<Void> createAsync(T instance) {
		return createAsync(instance, CallbackUtils.empty());
	}

	/**
	 * The asynchronous version of the {@link #create(BaseEntity)} method without custom exception handling.
	 *
	 * @param instance The instance to create on the database asynchronously.
	 * @param callback The action to apply to the result, once it is computed.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #create(BaseEntity)
	 */
	public CompletableFuture<Void> createAsync(T instance, Consumer<? super Long> callback) {
		return createAsync(instance, callback, null);
	}

	/**
	 * The asynchronous version of the {@link #create(BaseEntity)} method.
	 *
	 * @param instance          The instance to create on the database asynchronously.
	 * @param callback          The action to apply to the result, once it is computed.
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #create(BaseEntity)
	 */
	public CompletableFuture<Void> createAsync(T instance, Consumer<? super Long> callback, Consumer<SQLException> exceptionHandling) {
		return CompletableFuture.supplyAsync(supplierHandling(() -> super.create(instance), exceptionHandling)).thenAcceptAsync(callback);
	}

	/**
	 * The asynchronous version of the {@link #create(BaseEntity[])} method without custom exception handling.
	 *
	 * @param instances The instance to create on the database asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #create(BaseEntity[])
	 */
	@SuppressWarnings("unchecked")
	public CompletableFuture<Void> createAsync(T... instances) {
		return createAsync(Arrays.asList(instances), null);
	}

	/**
	 * The asynchronous version of the {@link #create(BaseEntity[])} method.
	 *
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @param instances         The instance to create on the database asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #create(BaseEntity[])
	 */
	@SuppressWarnings("unchecked")
	public CompletableFuture<Void> createAsync(Consumer<SQLException> exceptionHandling, T... instances) {
		return createAsync(Arrays.asList(instances), exceptionHandling);
	}

	/**
	 * The asynchronous version of the {@link #create(List)} method without custom exception handling.
	 *
	 * @param instances The instance to create on the database asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #create(List)
	 */
	public CompletableFuture<Void> createAsync(List<T> instances) {
		return createAsync(instances, null);
	}

	/**
	 * The asynchronous version of the {@link #create(List)} method.
	 *
	 * @param instances         The instance to create on the database asynchronously.
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #create(List)
	 */
	public CompletableFuture<Void> createAsync(List<T> instances, Consumer<SQLException> exceptionHandling) {
		return CompletableFuture.runAsync(runnableHandling(() -> super.create(instances), exceptionHandling));
	}

	//endregion

	//region Read

	//region Count

	/**
	 * The asynchronous version of the {@link #count()} method.
	 *
	 * @param callback The action to apply to the result, once it is computed.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #count()
	 */
	public CompletableFuture<Void> countAsync(Consumer<? super Long> callback) {
		return CompletableFuture.supplyAsync(super::count).thenAcceptAsync(callback);
	}

	/**
	 * The asynchronous version of the {@link #count(SqlPredicate)} method.
	 *
	 * @param predicate The condition to count by.
	 * @param callback  The action to apply to the result, once it is computed.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #count(SqlPredicate)
	 */
	public CompletableFuture<Void> countAsync(SqlPredicate<T> predicate, Consumer<? super Long> callback) {
		return CompletableFuture.supplyAsync(() -> super.count(predicate)).thenAcceptAsync(callback);
	}

	//endregion

	//region Any

	/**
	 * The asynchronous version of the {@link #any()} method.
	 *
	 * @param callback The action to apply to the result, once it is computed.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #any()
	 */
	public CompletableFuture<Void> anyAsync(Consumer<? super Boolean> callback) {
		return CompletableFuture.supplyAsync(super::any).thenAcceptAsync(callback);
	}

	/**
	 * The asynchronous version of the {@link #any(SqlPredicate)} method.
	 *
	 * @param predicate The condition to check for.
	 * @param callback  The action to apply to the result, once it is computed.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #any(SqlPredicate)
	 */
	public CompletableFuture<Void> anyAsync(SqlPredicate<T> predicate, Consumer<? super Boolean> callback) {
		return CompletableFuture.supplyAsync(() -> super.any(predicate)).thenAcceptAsync(callback);
	}

	//endregion

	//region HasDuplicates

	/**
	 * The asynchronous version of the {@link #hasDuplicates(SqlFunction)} method.
	 *
	 * @param column   The column to check for duplicates in.
	 * @param callback The action to apply to the result, once it is computed.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #hasDuplicates(SqlFunction)
	 */
	public CompletableFuture<Void> hasDuplicatesAsync(SqlFunction<T, ?> column, Consumer<? super Boolean> callback) {
		return CompletableFuture.supplyAsync(() -> super.hasDuplicates(column)).thenAcceptAsync(callback);
	}

	//endregion

	//region Query

	/**
	 * The asynchronous version of the {@link #getSingle(SqlPredicate)} method.
	 *
	 * @param predicate The condition to get the result by.
	 * @param callback  The action to apply to the result, once it is computed.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #getSingle(SqlPredicate)
	 */
	public CompletableFuture<Void> getSingleAsync(SqlPredicate<T> predicate, Consumer<? super Optional<T>> callback) {
		return CompletableFuture.supplyAsync(() -> super.getSingle(predicate)).thenAcceptAsync(callback);
	}

	/**
	 * The asynchronous version of the {@link #getById(long)} method.
	 *
	 * @param id       An id to find a specific entity by.
	 * @param callback The action to apply to the result, once it is computed.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #getById(long)
	 */
	public CompletableFuture<Void> getByIdAsync(long id, Consumer<? super Optional<T>> callback) {
		return getSingleAsync(x -> x.getId() == id, callback);
	}

	/**
	 * The asynchronous version of the {@link #getAll()} method.
	 *
	 * @param callback The action to apply to the result, once it is computed.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #getAll()
	 */
	public CompletableFuture<Void> getAllAsync(Consumer<? super List<T>> callback) {
		return createQuery().toListAsync(callback);
	}

	/**
	 * The asynchronous version of the {@link #getAll(SqlFunction)} method.
	 *
	 * @param callback The action to apply to the result, once it is computed.
	 * @param orderBy  The property to order by.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #getAll(SqlFunction)
	 */
	public CompletableFuture<Void> getAllAsync(Consumer<? super List<T>> callback, SqlFunction<T, ?> orderBy) {
		return createQuery().orderBy(orderBy).toListAsync(callback);
	}

	/**
	 * The asynchronous version of the {@link #getAll(SqlFunction[])} method.
	 *
	 * @param callback The action to apply to the result, once it is computed.
	 * @param orderBy  The properties to order by.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #getAll(SqlFunction[])
	 */
	@SuppressWarnings("unchecked")
	public CompletableFuture<Void> getAllAsync(Consumer<? super List<T>> callback, SqlFunction<T, ?>... orderBy) {
		return getAllAsync(callback, OrderTypes.ASCENDING, orderBy);
	}

	/**
	 * The asynchronous version of the {@link #getAll(OrderTypes, SqlFunction)} method.
	 *
	 * @param callback    The action to apply to the result, once it is computed.
	 * @param orderBy     The property to order by.
	 * @param sortingType The order direction. Can be either ascending or descending.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #getAll(OrderTypes, SqlFunction)
	 */
	public CompletableFuture<Void> getAllAsync(Consumer<? super List<T>> callback, OrderTypes sortingType, SqlFunction<T, ?> orderBy) {
		return createQuery().orderBy(sortingType, orderBy).toListAsync(callback);
	}

	/**
	 * The asynchronous version of the {@link #getAll(OrderTypes, SqlFunction[])} method.
	 *
	 * @param callback    The action to apply to the result, once it is computed.
	 * @param orderBy     The properties to order by.
	 * @param sortingType The order direction. Can be either ascending or descending.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #getAll(OrderTypes, SqlFunction[])
	 */
	@SuppressWarnings("unchecked")
	public CompletableFuture<Void> getAllAsync(Consumer<? super List<T>> callback, OrderTypes sortingType, SqlFunction<T, ?>... orderBy) {
		return createQuery().orderBy(sortingType, orderBy).toListAsync(callback);
	}

	//endregion

	//endregion

	//region Update

	/**
	 * The asynchronous version of the {@link #update(BaseEntity)} method without custom exception handling.
	 *
	 * @param instance The instance to update asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #update(BaseEntity)
	 */
	public CompletableFuture<Void> updateAsync(T instance) {
		return updateAsync(instance, null);
	}

	/**
	 * The asynchronous version of the {@link #update(BaseEntity)} method.
	 *
	 * @param instance          The instance to update asynchronously.
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #update(BaseEntity)
	 */
	public CompletableFuture<Void> updateAsync(T instance, Consumer<SQLException> exceptionHandling) {
		return CompletableFuture.runAsync(runnableHandling(() -> super.update(instance), exceptionHandling));
	}

	/**
	 * The asynchronous version of the {@link #update(BaseEntity[])} without custom exception handling.
	 *
	 * @param instances The instances to update asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #update(BaseEntity[])
	 */
	@SuppressWarnings("unchecked")
	public CompletableFuture<Void> updateAsync(T... instances) {
		return updateAsync(Arrays.asList(instances), null);
	}

	/**
	 * The asynchronous version of the {@link #update(BaseEntity[])}.
	 *
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @param instances         The instances to update asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #update(BaseEntity[])
	 */
	@SuppressWarnings("unchecked")
	public CompletableFuture<Void> updateAsync(Consumer<SQLException> exceptionHandling, T... instances) {
		return updateAsync(Arrays.asList(instances), exceptionHandling);
	}

	/**
	 * The asynchronous version of the {@link #update(List)} without custom exception handling.
	 *
	 * @param instances The instances to update asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #update(List)
	 */
	public CompletableFuture<Void> updateAsync(List<T> instances) {
		return updateAsync(instances, null);
	}

	/**
	 * The asynchronous version of the {@link #update(List)}.
	 *
	 * @param instances         The instances to update asynchronously.
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #update(List)
	 */
	public CompletableFuture<Void> updateAsync(List<T> instances, Consumer<SQLException> exceptionHandling) {
		return CompletableFuture.runAsync(runnableHandling(() -> super.update(instances), exceptionHandling));
	}

	/**
	 * The asynchronous version of the {@link #update(long, SqlFunction, Object)} method without custom exception handling.
	 *
	 * @param entityId The id of the instance to update asynchronously.
	 * @param column   The column of the entity to update.
	 * @param newValue The new value of the column.
	 * @param <R>      The data type of the column to update. It must be the same as the data type of the new value.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #update(long, SqlFunction, Object)
	 */
	public <R> CompletableFuture<Void> updateAsync(long entityId, SqlFunction<T, R> column, R newValue) {
		return updateAsync(x -> x.getId() == entityId, column, newValue, null);
	}

	/**
	 * The asynchronous version of the {@link #update(long, SqlFunction, Object)} method.
	 *
	 * @param entityId          The id of the instance to update asynchronously.
	 * @param column            The column of the entity to update.
	 * @param newValue          The new value of the column.
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @param <R>               The data type of the column to update. It must be the same as the data type of the new value.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #update(long, SqlFunction, Object)
	 */
	public <R> CompletableFuture<Void> updateAsync(long entityId, SqlFunction<T, R> column, R newValue, Consumer<SQLException> exceptionHandling) {
		return updateAsync(x -> x.getId() == entityId, column, newValue, exceptionHandling);
	}

	/**
	 * The asynchronous version of the {@link #update(SqlPredicate, SqlFunction, Object)} method without custom exception handling.
	 *
	 * @param condition The condition to find records by which will be updated asynchronously.
	 * @param column    The column of the entity to update.
	 * @param newValue  The new value of the column.
	 * @param <R>       The data type of the column to update. It must be the same as the data type of the new value.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #update(SqlPredicate, SqlFunction, Object)
	 */
	public <R> CompletableFuture<Void> updateAsync(SqlPredicate<T> condition, SqlFunction<T, R> column, R newValue) {
		return updateAsync(condition, column, newValue, null);
	}

	/**
	 * The asynchronous version of the {@link #update(SqlPredicate, SqlFunction, Object)} method.
	 *
	 * @param condition         The condition to find records by which will be updated asynchronously.
	 * @param column            The column of the entity to update.
	 * @param newValue          The new value of the column.
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @param <R>               The data type of the column to update. It must be the same as the data type of the new value.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #update(SqlPredicate, SqlFunction, Object)
	 */
	public <R> CompletableFuture<Void> updateAsync(SqlPredicate<T> condition, SqlFunction<T, R> column, R newValue, Consumer<SQLException> exceptionHandling) {
		return CompletableFuture.runAsync(runnableHandling(() -> super.update(condition, column, newValue), exceptionHandling));
	}

	//endregion

	//region Delete

	/**
	 * The asynchronous version of the {@link #delete(BaseEntity)}  method without custom exception handling.
	 *
	 * @param instance The instance to delete asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #delete(BaseEntity)
	 */
	public CompletableFuture<Void> deleteAsync(T instance) {
		return deleteAsync(instance, null);
	}

	/**
	 * The asynchronous version of the {@link #delete(BaseEntity)} method.
	 *
	 * @param instance          The instance to delete asynchronously.
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #delete(BaseEntity)
	 */
	public CompletableFuture<Void> deleteAsync(T instance, Consumer<SQLException> exceptionHandling) {
		return CompletableFuture.runAsync(runnableHandling(() -> super.delete(instance), exceptionHandling));
	}

	/**
	 * The asynchronous version of the {@link #delete(long)} method without custom exception handling.
	 *
	 * @param id The id of the instance to delete asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #delete(long)
	 */
	public CompletableFuture<Void> deleteAsync(long id) {
		return deleteAsync(id, null);
	}

	/**
	 * The asynchronous version of the {@link #delete(long)} method.
	 *
	 * @param id                The id of the instance to delete asynchronously.
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #delete(long)
	 */
	public CompletableFuture<Void> deleteAsync(long id, Consumer<SQLException> exceptionHandling) {
		return CompletableFuture.runAsync(runnableHandling(() -> super.delete(id), exceptionHandling));
	}

	/**
	 * The asynchronous version of the {@link #delete(List)} method without custom exception handling.
	 *
	 * @param entities The instances to delete asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #delete(List)
	 */
	public CompletableFuture<Void> deleteAsync(List<T> entities) {
		return deleteAsync(entities, null);
	}

	/**
	 * The asynchronous version of the {@link #delete(List)} method.
	 *
	 * @param entities          The instances to delete asynchronously.
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #delete(List)
	 */
	public CompletableFuture<Void> deleteAsync(List<T> entities, Consumer<SQLException> exceptionHandling) {
		return CompletableFuture.runAsync(runnableHandling(() -> super.delete(entities), exceptionHandling));
	}

	/**
	 * The asynchronous version of the {@link #delete(BaseEntity[])} method without custom exception handling.
	 *
	 * @param entities The instances to delete asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #delete(BaseEntity[])
	 */
	@SuppressWarnings("unchecked")
	public CompletableFuture<Void> deleteAsync(T... entities) {
		return deleteAsync(null, entities);
	}

	/**
	 * The asynchronous version of the {@link #delete(BaseEntity[])} method.
	 *
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @param entities          The instances to delete asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #delete(BaseEntity[])
	 */
	@SuppressWarnings("unchecked")
	public CompletableFuture<Void> deleteAsync(Consumer<SQLException> exceptionHandling, T... entities) {
		return CompletableFuture.runAsync(runnableHandling(() -> super.delete(Arrays.asList(entities)), exceptionHandling));
	}

	/**
	 * The asynchronous version of the {@link #delete(long...)} method without custom exception handling.
	 *
	 * @param ids The ids of the instances to delete asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #delete(long...)
	 */
	public CompletableFuture<Void> deleteAsync(long... ids) {
		return deleteAsync(null, ids);
	}

	/**
	 * The asynchronous version of the {@link #delete(long...)} method.
	 *
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @param ids               The ids of the instances to delete asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #delete(long...)
	 */
	public CompletableFuture<Void> deleteAsync(Consumer<SQLException> exceptionHandling, long... ids) {
		return CompletableFuture.runAsync(runnableHandling(() -> super.delete(ids), exceptionHandling));
	}

	/**
	 * The asynchronous version of the {@link #delete(SqlPredicate)} method without custom exception handling.
	 *
	 * @param predicate A condition to delete records on the database by asynchronously.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #delete(SqlPredicate)
	 */
	public CompletableFuture<Void> deleteAsync(SqlPredicate<T> predicate) {
		return deleteAsync(predicate, null);
	}

	/**
	 * The asynchronous version of the {@link #delete(SqlPredicate)} method.
	 *
	 * @param predicate         A condition to delete records on the database by asynchronously.
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #delete(SqlPredicate)
	 */
	public CompletableFuture<Void> deleteAsync(SqlPredicate<T> predicate, Consumer<SQLException> exceptionHandling) {
		return CompletableFuture.runAsync(runnableHandling(() -> super.delete(predicate), exceptionHandling));
	}

	/**
	 * The asynchronous version of the {@link #truncateTable()} method without custom exception handling.
	 *
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #truncateTable()
	 */
	public CompletableFuture<Void> truncateTableAsync() {
		return truncateTableAsync(null);
	}

	/**
	 * The asynchronous version of the {@link #truncateTable()} method.
	 *
	 * @param exceptionHandling Custom exception handling for the checked exception thrown by the underlying method.
	 * @return A {@link CompletableFuture} which represents the asynchronous operation.
	 * @see #truncateTable()
	 */
	public CompletableFuture<Void> truncateTableAsync(Consumer<SQLException> exceptionHandling) {
		return CompletableFuture.runAsync(runnableHandling(super::truncateTable, exceptionHandling));
	}

	//endregion
}