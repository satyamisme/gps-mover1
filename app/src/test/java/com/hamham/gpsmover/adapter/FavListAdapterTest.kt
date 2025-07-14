package com.hamham.gpsmover.adapter

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hamham.gpsmover.R
import com.hamham.gpsmover.room.Favourite
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class FavListAdapterTest {

    private lateinit var adapter: FavListAdapter
    private lateinit var mockItemTouchHelper: androidx.recyclerview.widget.ItemTouchHelper

    @Before
    fun setUp() {
        adapter = FavListAdapter()
        mockItemTouchHelper = mock()
    }

    @Test
    fun testSetItems() {
        val items = listOf(
            Favourite(1, "Test Location 1", 1.0, 2.0, 0),
            Favourite(2, "Test Location 2", 3.0, 4.0, 1)
        )
        
        adapter.setItems(items)
        assertEquals(2, adapter.itemCount)
        assertEquals(items, adapter.getItems())
    }

    @Test
    fun testGetItems() {
        val items = listOf(Favourite(1, "Test", 1.0, 2.0, 0))
        adapter.setItems(items)
        assertEquals(items, adapter.getItems())
    }

    @Test
    fun testItemCount() {
        assertEquals(0, adapter.itemCount)
        
        val items = listOf(
            Favourite(1, "Test 1", 1.0, 2.0, 0),
            Favourite(2, "Test 2", 3.0, 4.0, 0)
        )
        adapter.setItems(items)
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun testSetItemTouchHelper() {
        adapter.setItemTouchHelper(mockItemTouchHelper)
        // This test verifies that the method doesn't throw an exception
        assertTrue(true)
    }

    @Test
    fun testOnItemClickCallback() {
        var clickedItem: Favourite? = null
        adapter.onItemClick = { item ->
            clickedItem = item
        }
        
        val testItem = Favourite(1, "Test", 1.0, 2.0, 0)
        adapter.setItems(listOf(testItem))
        
        // Simulate item click
        adapter.onItemClick?.invoke(testItem)
        
        assertEquals(testItem, clickedItem)
    }

    @Test
    fun testOnItemDeleteCallback() {
        var deletedItem: Favourite? = null
        adapter.onItemDelete = { item ->
            deletedItem = item
        }
        
        val testItem = Favourite(1, "Test", 1.0, 2.0, 0)
        adapter.setItems(listOf(testItem))
        
        // Simulate item delete
        adapter.onItemDelete?.invoke(testItem)
        
        assertEquals(testItem, deletedItem)
    }

    @Test
    fun testOnItemMoveCallback() {
        var fromPosition = -1
        var toPosition = -1
        adapter.onItemMove = { from, to ->
            fromPosition = from
            toPosition = to
        }
        
        adapter.setItems(listOf(
            Favourite(1, "Test 1", 1.0, 2.0, 0),
            Favourite(2, "Test 2", 3.0, 4.0, 1)
        ))
        
        // Simulate item move
        adapter.onItemMove?.invoke(0, 1)
        
        assertEquals(0, fromPosition)
        assertEquals(1, toPosition)
    }

    @Test
    fun testMoveItem() {
        val items = listOf(
            Favourite(1, "Test 1", 1.0, 2.0, 0),
            Favourite(2, "Test 2", 3.0, 4.0, 1),
            Favourite(3, "Test 3", 5.0, 6.0, 2)
        )
        adapter.setItems(items)
        
        adapter.moveItem(0, 2)
        
        val movedItems = adapter.getItems()
        assertEquals("Test 1", movedItems[2].address)
        assertEquals("Test 2", movedItems[0].address)
        assertEquals("Test 3", movedItems[1].address)
    }

    @Test
    fun testMoveItemInvalidPositions() {
        val items = listOf(Favourite(1, "Test", 1.0, 2.0, 0))
        adapter.setItems(items)
        
        // Test with invalid positions
        adapter.moveItem(-1, 0) // Should not crash
        adapter.moveItem(0, -1) // Should not crash
        adapter.moveItem(10, 0) // Should not crash
        adapter.moveItem(0, 10) // Should not crash
        
        // Items should remain unchanged
        assertEquals(1, adapter.itemCount)
    }
} 