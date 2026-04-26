# 数组中第K大元素 - Java实现详解

## 方法对比

| 方法 | 时间复杂度 | 空间复杂度 | 推荐度 | 适用场景 |
|------|-----------|-----------|--------|---------|
| **小顶堆** | O(n log k) | O(k) | ⭐⭐⭐⭐⭐ | **面试首选** |
| 快速选择 | O(n) 平均 | O(1) | ⭐⭐⭐⭐ | 追求极致性能 |
| 排序 | O(n log n) | O(1) | ⭐⭐ | 简单但不够优 |

---

## 方法1：小顶堆（推荐⭐⭐⭐⭐⭐）

### 核心思路
- 维护一个大小为k的**小顶堆**
- 堆顶是第k大的元素
- 遍历数组，大于堆顶就替换

### 为什么用小顶堆？
- 小顶堆的堆顶是最小值
- 保持k个最大的元素，堆顶就是第k大

### 代码实现

```java
import java.util.PriorityQueue;

public class Solution {
    /**
     * 找到数组中第K大的元素
     * @param nums 数组
     * @param k 第k大
     * @return 第k大的元素
     */
    public int findKthLargest(int[] nums, int k) {
        // 创建小顶堆（Java的PriorityQueue默认就是小顶堆）
        PriorityQueue<Integer> heap = new PriorityQueue<>();
        
        // 遍历数组
        for (int num : nums) {
            heap.offer(num);  // 加入堆
            
            // 如果堆大小超过k，弹出最小的
            if (heap.size() > k) {
                heap.poll();
            }
        }
        
        // 堆顶就是第k大的元素
        return heap.peek();
    }
    
    // 测试
    public static void main(String[] args) {
        Solution solution = new Solution();
        
        // 测试用例1
        int[] nums1 = {3, 2, 1, 5, 6, 4};
        System.out.println(solution.findKthLargest(nums1, 2));  // 输出：5
        
        // 测试用例2
        int[] nums2 = {3, 2, 3, 1, 2, 4, 5, 5, 6};
        System.out.println(solution.findKthLargest(nums2, 4));  // 输出：4
    }
}
```

### 执行过程示例

**输入**：nums = [3, 2, 1, 5, 6, 4], k = 2

```
初始：heap = []

遍历 3：heap = [3]
遍历 2：heap = [2, 3]
遍历 1：heap = [1, 2, 3] → size > 2，弹出1 → heap = [2, 3]
遍历 5：heap = [2, 3, 5] → size > 2，弹出2 → heap = [3, 5]
遍历 6：heap = [3, 5, 6] → size > 2，弹出3 → heap = [5, 6]
遍历 4：heap = [4, 5, 6] → size > 2，弹出4 → heap = [5, 6]

结果：heap.peek() = 5（第2大）
```

### 复杂度分析
- **时间复杂度**：O(n log k)
  - 遍历n个元素：O(n)
  - 每次堆操作：O(log k)
- **空间复杂度**：O(k)
  - 堆的大小固定为k

### 优势
- ✅ 时间复杂度优秀（比排序快）
- ✅ 空间复杂度小（只需要k个元素）
- ✅ 代码简洁易懂
- ✅ 面试官最喜欢的解法

---

## 方法2：快速选择（进阶）

### 核心思路
- 基于快速排序的分区思想
- 每次分区后，pivot左边都比它小，右边都比它大
- 如果pivot位置刚好是第k大，直接返回

### 代码实现

```java
import java.util.Random;

public class Solution {
    private Random random = new Random();
    
    public int findKthLargest(int[] nums, int k) {
        // 第k大 = 第(n-k)小（从0开始）
        return quickSelect(nums, 0, nums.length - 1, nums.length - k);
    }
    
    private int quickSelect(int[] nums, int left, int right, int k) {
        // 随机选择pivot（避免最坏情况）
        int pivotIndex = left + random.nextInt(right - left + 1);
        
        // 分区
        pivotIndex = partition(nums, left, right, pivotIndex);
        
        // 判断位置
        if (pivotIndex == k) {
            return nums[pivotIndex];
        } else if (pivotIndex < k) {
            return quickSelect(nums, pivotIndex + 1, right, k);
        } else {
            return quickSelect(nums, left, pivotIndex - 1, k);
        }
    }
    
    private int partition(int[] nums, int left, int right, int pivotIndex) {
        int pivot = nums[pivotIndex];
        
        // 把pivot移到最右边
        swap(nums, pivotIndex, right);
        
        // 分区
        int storeIndex = left;
        for (int i = left; i < right; i++) {
            if (nums[i] < pivot) {
                swap(nums, i, storeIndex);
                storeIndex++;
            }
        }
        
        // 把pivot放到正确位置
        swap(nums, storeIndex, right);
        
        return storeIndex;
    }
    
    private void swap(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }
    
    // 测试
    public static void main(String[] args) {
        Solution solution = new Solution();
        int[] nums = {3, 2, 1, 5, 6, 4};
        System.out.println(solution.findKthLargest(nums, 2));  // 输出：5
    }
}
```

### 复杂度分析
- **时间复杂度**：O(n) 平均，O(n²) 最坏
- **空间复杂度**：O(1)

### 优势
- ✅ 平均时间复杂度最优
- ✅ 空间复杂度O(1)

### 劣势
- ❌ 代码复杂
- ❌ 最坏情况O(n²)
- ❌ 面试时容易写错

---

## 方法3：排序（不推荐）

### 代码实现

```java
import java.util.Arrays;

public class Solution {
    public int findKthLargest(int[] nums, int k) {
        Arrays.sort(nums);
        return nums[nums.length - k];
    }
}
```

### 复杂度分析
- **时间复杂度**：O(n log n)
- **空间复杂度**：O(1)

### 劣势
- ❌ 时间复杂度不够优
- ❌ 面试官会追问"能优化吗？"

---

## 面试建议

### 推荐写法顺序

1. **首选：小顶堆**
   - 代码简洁
   - 复杂度优秀
   - 不容易出错
   - 面试官最认可

2. **进阶：快速选择**
   - 如果面试官追问"能更快吗？"
   - 说出思路即可，不一定要写完整代码

3. **不推荐：排序**
   - 只能作为最初的思路
   - 面试官会让你优化

### 面试话术

**第一步：说出思路**
```
"我用小顶堆来做。维护一个大小为k的小顶堆，堆顶就是第k大的元素。
遍历数组，如果元素大于堆顶，就替换。最后堆顶就是答案。
时间复杂度O(n log k)，空间复杂度O(k)。"
```

**第二步：边写边讲**
```
"首先创建一个PriorityQueue，Java默认就是小顶堆。
然后遍历数组，每次加入元素后，如果堆大小超过k，就弹出最小的。
最后返回堆顶元素。"
```

**第三步：分析复杂度**
```
"时间复杂度：遍历n个元素，每次堆操作log k，总共O(n log k)。
空间复杂度：堆的大小固定为k，所以是O(k)。"
```

**第四步：测试用例**
```
"测试一下：[3,2,1,5,6,4]，k=2
堆的变化：[] → [3] → [2,3] → [2,3] → [3,5] → [5,6] → [5,6]
结果是5，正确。"
```

---

## 常见追问

### Q1：为什么用小顶堆而不是大顶堆？

**回答**：
"小顶堆的堆顶是最小值。我们维护k个最大的元素，堆顶就是第k大。
如果用大顶堆，堆顶是最大值，无法直接得到第k大。"

### Q2：能优化到O(n)吗？

**回答**：
"可以用快速选择算法，平均O(n)。基于快速排序的分区思想，每次分区后判断pivot位置，如果刚好是第k大就返回。但代码比较复杂，而且最坏情况是O(n²)。"

### Q3：如果k很大怎么办？

**回答**：
"如果k接近n，可以找第(n-k)小的元素，这样堆的大小是(n-k)，更小。
或者直接排序，时间复杂度O(n log n)，但空间复杂度O(1)。"

### Q4：如果数组很大，内存放不下怎么办？

**回答**：
"可以用外部排序或者分治。把数组分成多个块，每个块找第k大，然后合并结果。
或者用采样 + 分区的方法，先估计第k大的范围，再精确查找。"

---

## 总结

### 面试必背

1. **小顶堆写法**（必须会写）
2. **时间复杂度**：O(n log k)
3. **空间复杂度**：O(k)
4. **为什么用小顶堆**：堆顶是第k大

### 加分项

1. 能说出快速选择的思路
2. 能分析不同k值的优化方案
3. 能处理追问

**记住：面试时先写小顶堆，简洁、正确、不容易出错！** 🚀
