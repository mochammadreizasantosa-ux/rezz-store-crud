package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ---------- Palet pastel ----------
val Ink = Color(0xFF3B3552)
val Lavender = Color(0xFFDCD6F7)
val Mint = Color(0xFFCDEFE3)
val Peach = Color(0xFFFFD9C7)
val Sky = Color(0xFFCFE8FF)
val Butter = Color(0xFFFFF3C4)
val Pink = Color(0xFFFFB0E5)
val Cream = Color(0xFFFFFBF7)
val CoralRed = Color(0xFFFF6B6B)
val Pastels = listOf(Mint, Peach, Sky, Butter, Pink, Lavender)

// ---------- Model Data ----------
data class Product(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val discountPercentage: Double,
    val rating: Double,
    val stock: Int,
    val brand: String,
    val category: String,
    val thumbnail: String,
    val images: List<String>
)

// ---------- Helper HTTP Backend CRUD (DummyJSON) ----------
object ApiClient {
    private const val BASE_URL = "https://dummyjson.com/products"

    suspend fun execute(method: String, endpoint: String, jsonBody: String? = null): String {
        return withContext(Dispatchers.IO) {
            val url = URL(if (endpoint.startsWith("http")) endpoint else "$BASE_URL$endpoint")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.connectTimeout = 12000
            conn.readTimeout = 12000

            if (jsonBody != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { os ->
                    os.write(jsonBody.toByteArray(Charsets.UTF_8))
                }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
            stream.bufferedReader().use { it.readText() }
        }
    }
}

fun parseSingleProduct(o: JSONObject): Product {
    val imgs = o.optJSONArray("images")
    return Product(
        id = o.optInt("id", (1000..9999).random()),
        title = o.optString("title", "Untitled Product"),
        description = o.optString("description", ""),
        price = o.optDouble("price", 0.0),
        discountPercentage = o.optDouble("discountPercentage", 0.0),
        rating = o.optDouble("rating", 4.5),
        stock = o.optInt("stock", 10),
        brand = o.optString("brand", "rezz store"),
        category = o.optString("category", "general"),
        thumbnail = o.optString("thumbnail", "https://cdn.dummyjson.com/products/images/beauty/Essence%20Mascara%20Lash%20Princess/thumbnail.png"),
        images = List(imgs?.length() ?: 0) { idx -> imgs!!.getString(idx) }
    )
}

fun parseProducts(json: String): List<Product> {
    val arr = JSONObject(json).getJSONArray("products")
    return List(arr.length()) { i ->
        parseSingleProduct(arr.getJSONObject(i))
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                RezzStoreScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RezzStoreScreen() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog States
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // 1. READ: Ambil Data Produk dari Backend
    fun loadData(query: String = "") {
        isLoading = true
        scope.launch {
            try {
                val endpoint = if (query.trim().isEmpty()) {
                    "?limit=100"
                } else {
                    "/search?q=${query.trim()}"
                }
                val response = ApiClient.execute("GET", endpoint)
                products = parseProducts(response)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Gagal memuat data dari backend: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // 2. CREATE: Tambah Produk Baru ke Backend
    fun handleAddProduct(title: String, price: Double, category: String, description: String) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("title", title)
                    put("price", price)
                    put("category", category)
                    put("description", description)
                }.toString()

                val response = ApiClient.execute("POST", "/add", json)
                val newProd = parseSingleProduct(JSONObject(response))

                // Tambahkan langsung ke daftar teratas
                products = listOf(newProd) + products
                snackbarHostState.showSnackbar("✅ Berhasil menambah '${newProd.title}' ke backend!")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("❌ Gagal menambah produk: ${e.message}")
            }
        }
    }

    // 3. UPDATE: Edit Produk ke Backend
    fun handleUpdateProduct(id: Int, title: String, price: Double, category: String, description: String) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("title", title)
                    put("price", price)
                    put("category", category)
                    put("description", description)
                }.toString()

                val response = ApiClient.execute("PUT", "/$id", json)
                val updatedProd = parseSingleProduct(JSONObject(response))

                // Update di daftar produk
                products = products.map { if (it.id == id) updatedProd else it }
                snackbarHostState.showSnackbar("✅ Berhasil mengupdate '${updatedProd.title}' di backend!")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("❌ Gagal update produk: ${e.message}")
            }
        }
    }

    // 4. DELETE: Hapus Produk dari Backend
    fun handleDeleteProduct(product: Product) {
        scope.launch {
            try {
                ApiClient.execute("DELETE", "/${product.id}")
                products = products.filter { it.id != product.id }
                snackbarHostState.showSnackbar("🗑️ Produk '${product.title}' berhasil dihapus!")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("❌ Gagal menghapus produk: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val categories = remember(products) {
        listOf("all") + products.map { it.category }.distinct()
    }

    val filteredProducts = remember(products, selectedCategory) {
        if (selectedCategory == "all") products
        else products.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Scaffold(
        containerColor = Cream,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Ink,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Produk"
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header Toko
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Lavender),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ShoppingBag,
                            contentDescription = "Logo",
                            tint = Ink
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "rezz store",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        Text(
                            text = "Toko produk pilihan (CRUD)",
                            fontSize = 11.sp,
                            color = Ink.copy(alpha = 0.6f)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Cart",
                        tint = Ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar Fungsional
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    loadData(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                placeholder = {
                    Text(
                        "Cari produk, laptop, mouse...",
                        fontSize = 14.sp,
                        color = Ink.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Ink.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            loadData("")
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Kategori Pastel Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = cat.equals(selectedCategory, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Mint else Color.White)
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Ink
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Grid Produk dengan Tombol Edit & Delete
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Ink)
                }
            } else if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Produk tidak ditemukan", color = Ink)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(filteredProducts) { index, product ->
                        val cardBg = Pastels[index % Pastels.size]
                        ProductCard(
                            product = product,
                            bgColor = cardBg,
                            onClick = { selectedProduct = product },
                            onEdit = { productToEdit = product },
                            onDelete = { productToDelete = product }
                        )
                    }
                }
            }
        }
    }

    // Dialog Detail Produk
    selectedProduct?.let { product ->
        ProductDetailDialog(
            product = product,
            onDismiss = { selectedProduct = null },
            onEdit = {
                productToEdit = product
                selectedProduct = null
            },
            onDelete = {
                productToDelete = product
                selectedProduct = null
            }
        )
    }

    // Dialog Tambah Produk (CREATE)
    if (showAddDialog) {
        ProductFormDialog(
            title = "Tambah Produk Baru",
            initialProduct = null,
            onDismiss = { showAddDialog = false },
            onSave = { title, price, category, desc ->
                handleAddProduct(title, price, category, desc)
                showAddDialog = false
            }
        )
    }

    // Dialog Edit Produk (UPDATE)
    productToEdit?.let { product ->
        ProductFormDialog(
            title = "Edit Produk #${product.id}",
            initialProduct = product,
            onDismiss = { productToEdit = null },
            onSave = { title, price, category, desc ->
                handleUpdateProduct(product.id, title, price, category, desc)
                productToEdit = null
            }
        )
    }

    // Dialog Konfirmasi Hapus (DELETE)
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Konfirmasi Hapus", fontWeight = FontWeight.Bold, color = Ink) },
            text = { Text("Apakah Anda yakin ingin menghapus '${product.title}' dari backend?") },
            confirmButton = {
                Button(
                    onClick = {
                        handleDeleteProduct(product)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Batal", color = Ink)
                }
            },
            containerColor = Cream,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ---------- Komponen Card Produk dengan Aksi Edit & Hapus ----------
@Composable
fun ProductCard(
    product: Product,
    bgColor: Color,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = product.thumbnail,
                    contentDescription = product.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(6.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${product.price}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFF4A261),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = product.rating.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Baris Tombol Aksi Edit & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Ink,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = CoralRed,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

// ---------- Dialog Form Tambah / Edit Produk ----------
@Composable
fun ProductFormDialog(
    title: String,
    initialProduct: Product?,
    onDismiss: () -> Unit,
    onSave: (title: String, price: Double, category: String, desc: String) -> Unit
) {
    var titleInput by remember { mutableStateOf(initialProduct?.title ?: "") }
    var priceInput by remember { mutableStateOf(initialProduct?.price?.toString() ?: "") }
    var categoryInput by remember { mutableStateOf(initialProduct?.category ?: "beauty") }
    var descInput by remember { mutableStateOf(initialProduct?.description ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Cream),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Ink)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Nama Produk") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Harga ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = categoryInput,
                    onValueChange = { categoryInput = it },
                    label = { Text("Kategori (mis: beauty, laptops, fragrances)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = descInput,
                    onValueChange = { descInput = it },
                    label = { Text("Deskripsi") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val parsedPrice = priceInput.toDoubleOrNull() ?: 0.0
                        if (titleInput.isNotBlank()) {
                            onSave(titleInput, parsedPrice, categoryInput, descInput)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Simpan ke Backend",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ---------- Dialog Detail Produk Lengkap ----------
@Composable
fun ProductDetailDialog(
    product: Product,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Cream),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Lavender)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = product.category.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Ink)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = product.thumbnail,
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize().padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = product.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$${product.price}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Mint)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Stok: ${product.stock}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Deskripsi:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.description,
                    fontSize = 13.sp,
                    color = Ink.copy(alpha = 0.75f),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Tombol Aksi CRUD di Halaman Detail
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = Lavender),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Ink, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Edit", fontWeight = FontWeight.Bold, color = Ink)
                    }

                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Hapus", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
