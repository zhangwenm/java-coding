#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据对比脚本：对比CSV和JSON中的设备数据
将CSV中在JSON中存在的数据标记并导出为Excel
"""

import csv
import json
import pandas as pd
from pathlib import Path


def load_csv_data(csv_path):
    """加载CSV文件数据"""
    csv_data = []
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            csv_data.append(row)
    return csv_data


def load_json_data(json_path):
    """加载JSON文件数据，提取所有product_id"""
    product_ids = set()
    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
        for item in data:
            if '_source' in item and 'productId' in item['_source']:
                product_ids.add(item['_source']['productId'])
    return product_ids


def compare_and_mark(csv_data, json_product_ids):
    """对比数据并标记"""
    for row in csv_data:
        # 使用 device_id 和 product_id 进行匹配
        device_id = row.get('device_id', '').strip()
        product_id = row.get('product_id', '').strip()

        # 标记是否在JSON中存在
        exists_in_json = '否'
        if device_id in json_product_ids or product_id in json_product_ids:
            exists_in_json = '是'
        row['在JSON中存在'] = exists_in_json
    return csv_data


def export_to_excel(data, output_path):
    """导出为Excel"""
    df = pd.DataFrame(data)

    # 重新排列列，把"在JSON中存在"放在最后
    columns = list(df.columns)
    if '在JSON中存在' in columns:
        columns.remove('在JSON中存在')
        columns.append('在JSON中存在')
        df = df[columns]

    df.to_excel(output_path, index=False, engine='openpyxl')
    print(f"✅ 成功导出到: {output_path}")


def main():
    # 文件路径
    base_dir = Path(__file__).parent / "java-coding-worktest" / "src" / "main" / "resources"
    csv_path = base_dir / "腾讯云DMC_数据导出_1769412599092.csv"
    json_path = base_dir / "devicexiaodai.json"
    output_path = Path(__file__).parent / "数据对比结果.xlsx"

    print("=" * 60)
    print("开始对比CSV和JSON数据...")
    print("=" * 60)

    # 检查文件是否存在
    if not csv_path.exists():
        print(f"❌ CSV文件不存在: {csv_path}")
        return

    if not json_path.exists():
        print(f"❌ JSON文件不存在: {json_path}")
        return

    # 加载数据
    print(f"\n📖 正在读取CSV文件...")
    csv_data = load_csv_data(csv_path)
    print(f"   CSV文件共 {len(csv_data)} 条记录")

    print(f"\n📖 正在读取JSON文件...")
    json_product_ids = load_json_data(json_path)
    print(f"   JSON文件共 {len(json_product_ids)} 个不同的product_id")

    # 对比数据
    print(f"\n🔍 正在对比数据...")
    marked_data = compare_and_mark(csv_data, json_product_ids)

    # 统计结果
    exists_count = sum(1 for row in marked_data if row['在JSON中存在'] == '是')
    print(f"   CSV中在JSON中存在的记录: {exists_count} 条")
    print(f"   CSV中不在JSON中的记录: {len(marked_data) - exists_count} 条")

    # 导出Excel
    print(f"\n📊 正在导出Excel...")
    export_to_excel(marked_data, output_path)

    print("\n" + "=" * 60)
    print("✨ 处理完成!")
    print("=" * 60)


if __name__ == '__main__':
    main()
