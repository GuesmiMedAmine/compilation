auto identite(auto x) {
    return x;
}

auto calcul(int a) {
    auto res = a + 10;
    return res;
}

int main() {
    auto x = 42;
    auto y = true;

    int i = identite(x);
    bool b = identite(y);

    int[] tab;

    tab[0] = i;

    auto taille = calcul(5);
    tab[taille] = 123;

    print(tab);

    return 0;
}
