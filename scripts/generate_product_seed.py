"""
상품 대용량 테스트 데이터 생성 스크립트

사용법:
  python3 scripts/generate_product_seed.py > /tmp/products_seed.sql
  docker exec <mysql-container> mysql -u application -papplication loopers -e "source /tmp/products_seed.sql"

생성 데이터:
  - 10만건 상품
  - brand_id: 1~20 균등 분포
  - like_count: 0~5000 랜덤
  - price: 1000~100000 랜덤
  - stock_quantity: 0~1000 랜덤
"""
import random
import datetime

TOTAL = 100_000
CHUNK_SIZE = 5000
BRAND_COUNT = 20
MAX_LIKES = 5000

now = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S.000000')

for start in range(0, TOTAL, CHUNK_SIZE):
    end = min(start + CHUNK_SIZE, TOTAL)
    print("INSERT INTO products (brand_id, name, price, description, stock_quantity, like_count, created_at, updated_at, deleted_at) VALUES")
    vals = []
    for i in range(start + 1, end + 1):
        brand_id = random.randint(1, BRAND_COUNT)
        name = f'상품_{i:06d}'
        price = random.randint(1000, 100000)
        desc = f'설명_{i}'
        stock = random.randint(0, 1000)
        like_count = random.randint(0, MAX_LIKES)
        vals.append(f"({brand_id},'{name}',{price},'{desc}',{stock},{like_count},'{now}','{now}',NULL)")
    print(",\n".join(vals) + ";")
