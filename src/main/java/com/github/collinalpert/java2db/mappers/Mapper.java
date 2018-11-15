package com.github.collinalpert.java2db.mappers;

import com.github.collinalpert.java2db.entities.BaseEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Collin Alpert
 */
public interface Mapper<T extends BaseEntity> {

	Optional<T> map(ResultSet set) throws SQLException;

	List<T> mapToList(ResultSet set) throws SQLException;

	Stream<T> mapToStream(ResultSet set) throws SQLException;
}
