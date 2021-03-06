package com.mcdenny.easyshopug;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mcdenny.easyshopug.Common.Common;
import com.mcdenny.easyshopug.Model.Cart;
import com.mcdenny.easyshopug.Model.Product;
import com.mcdenny.easyshopug.Utils.Cons;
import com.mcdenny.easyshopug.Utils.Util;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ProductDetail extends AppCompatActivity {
    TextView prdName, prdDescription, prdPrice, prdOwner;
    ImageView prdImage;
    ImageView burgain;
    CollapsingToolbarLayout detailCollapsingToolbar;
    ElegantNumberButton numberButton;

    String productID = "";
    Product finalProduct;
    String cNm, cImg, cDesc, cPrice, cDisc;
    Button addToCart;
    FirebaseDatabase database;
    DatabaseReference productDetail;
    DatabaseReference cartDetail;
    String currentUserEmail;

    List<Product> prd_detail = new ArrayList<>();
    public static String mQty;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set the fonts
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/MontserratRegular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        setContentView(R.layout.activity_product_detail);
        Toolbar toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setHomeAsUpIndicator(R.drawable.back_ic);


        //firebase init
        database = FirebaseDatabase.getInstance();
        productDetail = database.getReference("Products");
        cartDetail = database.getReference("Cart");

        currentUserEmail = Util.cleanEmailKey(Common.current_user_email);

        //Getting the product list id from the product list activity
        if (getIntent() != null) {
            productID = getIntent().getStringExtra("ProductListID");
        }
        if (!productID.isEmpty()) {
            if (Common.isNetworkAvailable(getBaseContext())) {
                getProductDetail(productID);
            } else {
                Toast.makeText(ProductDetail.this, "Check your internet connection", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        //reference to views
        prdName = findViewById(R.id.product_detail_name);
        prdDescription = findViewById(R.id.product_description);
        prdPrice = findViewById(R.id.product_detail_price);
        prdImage = findViewById(R.id.product_detail_image);
        addToCart = findViewById(R.id.add_to_cart);
        numberButton = findViewById(R.id.add_subtract_button);
        prdOwner = findViewById(R.id.prd_added_by);
        //burgain = findViewById(R.id.img_bargain);

        addToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prd_detail = new ArrayList<Product>();
                //New cart
                //New cart
                Cart cart = new Cart(
                        currentUserEmail,
                        cNm,
                        cImg,
                        cDesc,
                        mQty,
                        numberButton.getNumber()

                );

                productDetail.child(productID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Product product = dataSnapshot.getValue(Product.class);

                        final Product updateProduct = new Product();
                        updateProduct.setAvailable("0");
                        updateProduct.setAddedBy(product.getAddedBy());
                        updateProduct.setBargained(product.getBargained());
                        updateProduct.setDescription(product.getDescription());
                        updateProduct.setImage(product.getImage());
                        updateProduct.setPrice(product.getPrice());
                        updateProduct.setName(product.getName());
                        updateProduct.setMenuid(product.getMenuid());

                        //productDetail.child(productID).setValue(updateProduct);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


                //sending the cart details to firebase
                cartDetail.child(currentUserEmail).push().setValue(cart);
                Snackbar.make(view, "Successfully added to the Cart", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        detailCollapsingToolbar = findViewById(R.id.collapsing_toolbar);
        detailCollapsingToolbar.setCollapsedTitleTextAppearance(R.style.CollapsedAppBar);
        detailCollapsingToolbar.setExpandedTitleTextAppearance(R.style.NavigatedAppBar);

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void getProductDetail(final String productID) {
        productDetail.child(productID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                finalProduct = dataSnapshot.getValue(Product.class);
                Picasso.with(getBaseContext()).load(finalProduct.getImage()).into(prdImage);

                detailCollapsingToolbar.setTitle(finalProduct.getName());//collapsing toolbar title
                prdName.setText(finalProduct.getName());
                prdDescription.setText("Description: \n" + finalProduct.getDescription());

                //Locale locale = new Locale("en", "UG");
                //NumberFormat numberFormat = NumberFormat.getCurrencyInstance(locale);
                int thePrice = (Integer.parseInt(finalProduct.getPrice()));
                prdPrice.setText(Cons.Vals.CURRENCY + Util.formatNumber(String.valueOf(thePrice)));
                mQty = finalProduct.getPrice();
                prdOwner.setText(finalProduct.getAddedBy());

                //to be sent to the cart
                cNm = finalProduct.getName();
                cImg = finalProduct.getImage();
                cDesc = "Description: \n" + finalProduct.getDescription();
                //cPrice = numberFormat.format(thePrice);
                cPrice = String.valueOf(thePrice);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Connect with me on facebook via: www.facebook.com/denislucaz");
            startActivity(Intent.createChooser(shareIntent, "Send Invite Via"));
            return true;
        }
        if (id == R.id.view_cart) {
            Intent cartIntent = new Intent(ProductDetail.this, CartDetail.class);
            startActivity(cartIntent);
        }
        if (id == R.id.nav_settings) {
            Intent settingIntent = new Intent(ProductDetail.this, SettingsActivity.class);
            startActivity(settingIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
