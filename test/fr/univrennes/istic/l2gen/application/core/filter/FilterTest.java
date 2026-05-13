package fr.univrennes.istic.l2gen.application.core.filter;

import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

public class FilterTest {

    @Test
    public void testSortFilterAscending() {
        Filter filter = Filter.sort(0, true);
        Assert.assertEquals(0, filter.getColumnIndex());
        Assert.assertEquals(FilterSort.ASCENDING, filter.getSort());
        Assert.assertFalse(filter.hasConditions());
    }

    @Test
    public void testSortFilterDescending() {
        Filter filter = Filter.sort(1, false);
        Assert.assertEquals(1, filter.getColumnIndex());
        Assert.assertEquals(FilterSort.DESCENDING, filter.getSort());
        Assert.assertFalse(filter.hasConditions());
    }

    @Test
    public void testSearchByString() {
        Filter filter = Filter.search(2, "test");
        Assert.assertEquals(2, filter.getColumnIndex());
        Assert.assertTrue(filter.hasConditions());
        Assert.assertEquals(1, filter.getConditions().size());
        Assert.assertEquals(FilterOperator.LIKE, filter.getConditions().get(0).operator());
        Assert.assertEquals("test", filter.getConditions().get(0).value());
    }

    @Test
    public void testSearchByDouble() {
        Filter filter = Filter.search(1, 42.5);
        Assert.assertEquals(1, filter.getColumnIndex());
        Assert.assertTrue(filter.hasConditions());
        Assert.assertEquals(1, filter.getConditions().size());
        Assert.assertEquals(FilterOperator.EQUAL, filter.getConditions().get(0).operator());
        Assert.assertEquals("42.5", filter.getConditions().get(0).value());
    }

    @Test
    public void testSearchByInt() {
        Filter filter = Filter.search(3, 100);
        Assert.assertEquals(3, filter.getColumnIndex());
        Assert.assertTrue(filter.hasConditions());
        Assert.assertEquals(1, filter.getConditions().size());
        Assert.assertEquals(FilterOperator.EQUAL, filter.getConditions().get(0).operator());
        Assert.assertEquals("100", filter.getConditions().get(0).value());
    }

    @Test
    public void testSearchByTimestamp() {
        Timestamp ts = Timestamp.valueOf("2026-05-06 14:30:00");
        Filter filter = Filter.search(0, ts);
        Assert.assertEquals(0, filter.getColumnIndex());
        Assert.assertTrue(filter.hasConditions());
        Assert.assertEquals(1, filter.getConditions().size());
        Assert.assertEquals(FilterOperator.EQUAL, filter.getConditions().get(0).operator());
        Assert.assertTrue(filter.getConditions().get(0).value().contains("2026-05-06"));
    }

    @Test
    public void testTopNByDouble() {
        Filter filter = Filter.topN(0, 50.0);
        Assert.assertEquals(0, filter.getColumnIndex());
        Assert.assertTrue(filter.hasConditions());
        Assert.assertEquals(1, filter.getConditions().size());
        Assert.assertEquals(FilterOperator.GREATER, filter.getConditions().get(0).operator());
        Assert.assertEquals("50.0", filter.getConditions().get(0).value());
        Assert.assertTrue(filter.hasRowLimitingEffect());
    }

    @Test
    public void testTopNByInt() {
        Filter filter = Filter.topN(1, 10);
        Assert.assertEquals(1, filter.getColumnIndex());
        Assert.assertTrue(filter.hasConditions());
        Assert.assertEquals(FilterOperator.GREATER, filter.getConditions().get(0).operator());
        Assert.assertEquals("10", filter.getConditions().get(0).value());
    }

    @Test
    public void testBottomNByDouble() {
        Filter filter = Filter.bottomN(2, 25.5);
        Assert.assertEquals(2, filter.getColumnIndex());
        Assert.assertTrue(filter.hasConditions());
        Assert.assertEquals(FilterOperator.LESS, filter.getConditions().get(0).operator());
        Assert.assertEquals("25.5", filter.getConditions().get(0).value());
        Assert.assertTrue(filter.hasRowLimitingEffect());
    }

    @Test
    public void testBottomNByInt() {
        Filter filter = Filter.bottomN(1, 5);
        Assert.assertEquals(1, filter.getColumnIndex());
        Assert.assertTrue(filter.hasConditions());
        Assert.assertEquals(FilterOperator.LESS, filter.getConditions().get(0).operator());
        Assert.assertEquals("5", filter.getConditions().get(0).value());
    }

    @Test
    public void testByRangeDouble() {
        Filter filter = Filter.byRange(0, 10.0, 100.0);
        Assert.assertEquals(0, filter.getColumnIndex());
        Assert.assertTrue(filter.hasConditions());
        Assert.assertEquals(2, filter.getConditions().size());

        FilterCondition first = filter.getConditions().get(0);
        FilterCondition second = filter.getConditions().get(1);

        Assert.assertEquals(FilterOperator.GREATER_EQUAL, first.operator());
        Assert.assertEquals("10.0", first.value());
        Assert.assertEquals(FilterOperator.LESS_EQUAL, second.operator());
        Assert.assertEquals("100.0", second.value());
    }

    @Test
    public void testByRangeInt() {
        Filter filter = Filter.byRange(1, 5, 15);
        Assert.assertEquals(1, filter.getColumnIndex());
        Assert.assertEquals(2, filter.getConditions().size());

        FilterCondition first = filter.getConditions().get(0);

        Assert.assertEquals(FilterOperator.GREATER_EQUAL, first.operator());
        Assert.assertEquals("5", first.value());
        Assert.assertEquals(FilterFunction.LENGTH, first.func());
    }

    @Test
    public void testByRangeTimestamp() {
        Timestamp minDate = Timestamp.valueOf("2026-01-01 00:00:00");
        Timestamp maxDate = Timestamp.valueOf("2026-12-31 23:59:59");

        Filter filter = Filter.byRange(0, minDate, maxDate);
        Assert.assertEquals(0, filter.getColumnIndex());
        Assert.assertEquals(2, filter.getConditions().size());
    }

    @Test
    public void testShowEmpty() {
        Filter filter = Filter.showEmpty(3);
        Assert.assertEquals(3, filter.getColumnIndex());
        Assert.assertTrue(filter.hasConditions());
        Assert.assertEquals(1, filter.getConditions().size());
        Assert.assertEquals(FilterOperator.IS_NULL, filter.getConditions().get(0).operator());
        Assert.assertTrue(filter.hasRowLimitingEffect());
    }

    @Test
    public void testHideEmpty() {
        Filter filter = Filter.hideEmpty(2);
        Assert.assertEquals(2, filter.getColumnIndex());
        Assert.assertTrue(filter.hasConditions());
        Assert.assertEquals(1, filter.getConditions().size());
        Assert.assertEquals(FilterOperator.NOT_NULL, filter.getConditions().get(0).operator());
        Assert.assertTrue(filter.hasRowLimitingEffect());
    }

    @Test
    public void testFilterOperatorLogic() {
        Filter filter = Filter.search(0, "value");
        Assert.assertEquals(FilterLogic.AND, filter.getOperator());

        filter.setOperator(FilterLogic.OR);
        Assert.assertEquals(FilterLogic.OR, filter.getOperator());
    }

    @Test
    public void testGetSQL() {
        Filter filter = Filter.search(0, "test");
        String sql = filter.getSQL("column_name");
        Assert.assertTrue(sql.contains("LIKE"));
        Assert.assertTrue(sql.contains("test"));
    }

    @Test
    public void testMultipleConditions() {
        Filter filter = new Filter(0);
        filter.add(new FilterCondition(FilterOperator.GREATER, "10"));
        filter.add(new FilterCondition(FilterOperator.LESS, "100"));

        Assert.assertEquals(2, filter.getConditions().size());
        Assert.assertTrue(filter.hasConditions());

        String sql = filter.getSQL("value");
        Assert.assertTrue(sql.contains("AND"));
    }

    @Test
    public void testSQLGenerationWithFunction() {
        Filter filter = Filter.byRange(0, 5, 10);
        String sql = filter.getSQL("text_column");
        // Should include LENGTH function for string range
        Assert.assertTrue(sql.contains("LENGTH") || sql.contains(">="));
    }

}
