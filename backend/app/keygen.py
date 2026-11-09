from app.signing import generate_seed

if __name__ == "__main__":
    print(f"PALMA_SIGNING_KEY={generate_seed()}")
