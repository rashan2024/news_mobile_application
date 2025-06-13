package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.databinding.ActivityNewsScreenBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class NewsScreenActivity extends AppCompatActivity {

    private ActivityNewsScreenBinding binding;
    private FirebaseAuth mAuth;
    private NewsAdapter newsAdapter;
    private List<NewsItem> allNewsItems;
    private String currentCategory = "Events";
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.parseColor("#28339E"));

        binding = ActivityNewsScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout, (v, insets) -> {
            int systemBarInsetsTop = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), systemBarInsetsTop, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        setSupportActionBar(binding.topAppBar);

        setupAllNewsData();
        setupRecyclerView();
        setupBottomNavigation();
        setupRefreshLayout();

        binding.bottomNavigation.setSelectedItemId(R.id.nav_events);
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (searchView != null && !searchView.isIconified()) {
                searchView.setIconified(true);
                searchView.onActionViewCollapsed();
            }

            int itemId = item.getItemId();
            if (itemId == R.id.nav_sports) {
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Sports");
                currentCategory = "Sports";
                newsAdapter.filter(currentCategory, "");
                return true;
            } else if (itemId == R.id.nav_academics) {
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Academics");
                currentCategory = "Academics";
                newsAdapter.filter(currentCategory, "");
                return true;
            } else if (itemId == R.id.nav_events) {
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Events");
                currentCategory = "Events";
                newsAdapter.filter(currentCategory, "");
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                newsAdapter.filter(currentCategory, query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                newsAdapter.filter(currentCategory, newText);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (searchView != null) {
            MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
            if (searchMenuItem != null) {
                searchMenuItem.setVisible(searchView.isIconified());
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_developer_details) {
            startActivity(new Intent(NewsScreenActivity.this, dev_infoActivity.class));
            return true;
        } else if (itemId == R.id.menu_user_info) {
            // --- THIS IS THE UPDATED PART (STEP 3) ---
            // Navigate to the new UserInfoActivity
            startActivity(new Intent(NewsScreenActivity.this, UserInfoActivity.class));
            return true;
        } else if (itemId == R.id.menu_logout) {
            mAuth.signOut();
            Intent intent = new Intent(NewsScreenActivity.this, loginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- Unchanged methods below ---
    private void setupAllNewsData() {
        allNewsItems = new ArrayList<>();
        allNewsItems.add(new NewsItem("NoorFeast 2025", "NoorFeast 2025 organized by Muslim Students of FOT...", "10 min ago", R.drawable.placeholder_img, "Events"));
        allNewsItems.add(new NewsItem("Annual Drama 'Kalayana'", "The university drama society presents 'Kalayana'.", "2 days ago", R.drawable.placeholder_img_3, "Events"));
        allNewsItems.add(new NewsItem("Job Fair 2025", "The annual career fair will be held at the main hall.", "1 week ago", R.drawable.placeholder_img_2, "Events"));
        allNewsItems.add(new NewsItem("Music Festival 'Rhythm'", "An evening of music and entertainment by university bands.", "3 weeks ago", R.drawable.placeholder_img, "Events"));
        allNewsItems.add(new NewsItem("New AI Course", "A new course on Artificial Intelligence to be launched next semester.", "1 hour ago", R.drawable.placeholder_img_2, "Academics"));
        allNewsItems.add(new NewsItem("Library Extended Hours", "The library will be open 24/7 during the exam period.", "1 day ago", R.drawable.placeholder_img, "Academics"));
        allNewsItems.add(new NewsItem("Research Symposium", "Call for papers for the annual student research symposium.", "4 days ago", R.drawable.placeholder_img_3, "Academics"));
        allNewsItems.add(new NewsItem("Scholarship Deadline", "Deadline for the merit scholarship application is approaching.", "2 weeks ago", R.drawable.placeholder_img_2, "Academics"));
        allNewsItems.add(new NewsItem("Cricket Tournament Finals", "The inter-faculty cricket finals will be held this Saturday.", "5 hours ago", R.drawable.placeholder_img_3, "Sports"));
        allNewsItems.add(new NewsItem("Basketball Team Wins", "Our basketball team won the national university championship.", "3 days ago", R.drawable.placeholder_img_2, "Sports"));
        allNewsItems.add(new NewsItem("Library Extended Hours", "The library will be open 24/7 during the exam period.", "1 day ago", R.drawable.placeholder_img, "Academics"));
        allNewsItems.add(new NewsItem("Research Symposium", "Call for papers for the annual student research symposium.", "4 days ago", R.drawable.placeholder_img_3, "Academics"));
        allNewsItems.add(new NewsItem("Scholarship Deadline", "Deadline for the merit scholarship application is approaching.", "2 weeks ago", R.drawable.placeholder_img_2, "Academics"));
        allNewsItems.add(new NewsItem("Cricket Tournament Finals", "The inter-faculty cricket finals will be held this Saturday.", "5 hours ago", R.drawable.placeholder_img_3, "Sports"));
        allNewsItems.add(new NewsItem("Basketball Team Wins", "Our basketball team won the national university championship.", "3 days ago", R.drawable.placeholder_img_2, "Sports"));
        allNewsItems.add(new NewsItem("Annual Road Race", "Registrations are now open for the annual university marathon.", "6 days ago", R.drawable.placeholder_img, "Sports"));
        allNewsItems.add(new NewsItem("Annual Road Race", "Registrations are now open for the annual university marathon.", "6 days ago", R.drawable.placeholder_img, "Sports"));
        allNewsItems.add(new NewsItem("Chess Competition", "Inter-university chess competition to be hosted next month.", "1 month ago", R.drawable.placeholder_img_3, "Sports"));
    }

    private void setupRecyclerView() {
        newsAdapter = new NewsAdapter(allNewsItems);
        binding.newsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.newsRecyclerView.setAdapter(newsAdapter);
    }

    private void setupRefreshLayout() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                binding.swipeRefreshLayout.setRefreshing(false);
            }, 2000);
        });
    }
}
