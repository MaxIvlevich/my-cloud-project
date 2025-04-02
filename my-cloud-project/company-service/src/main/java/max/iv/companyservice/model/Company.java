package max.iv.companyservice.model;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "company")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(precision = 19, scale = 4)
    private BigDecimal budget;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "company_employee", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "employee_id", nullable = false)
    @Builder.Default
    private List<Long> employeeIds = new ArrayList<>();
}
