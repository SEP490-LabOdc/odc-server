package com.odc.paymentservice.service;

import com.odc.common.constant.Role;
import com.odc.common.constant.Status;
import com.odc.common.exception.BusinessException;
import com.odc.paymentservice.dto.request.CreateDisbursementRequest;
import com.odc.paymentservice.dto.response.DisbursementCalculationResponse;
import com.odc.paymentservice.dto.response.LeaderDisbursementInfo;
import com.odc.paymentservice.entity.Disbursement;
import com.odc.paymentservice.entity.SystemConfig;
import com.odc.paymentservice.entity.Transaction;
import com.odc.paymentservice.entity.Wallet;
import com.odc.paymentservice.repository.DisbursementRepository;
import com.odc.paymentservice.repository.SystemConfigRepository;
import com.odc.paymentservice.repository.TransactionRepository;
import com.odc.paymentservice.repository.WalletRepository;
import com.odc.projectservice.v1.GetLeaderInfoRequest;
import com.odc.projectservice.v1.GetLeaderInfoResponse;
import com.odc.projectservice.v1.LeaderInfo;
import com.odc.projectservice.v1.ProjectServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisbursementServiceImpl implements DisbursementService {
    private final SystemConfigRepository systemConfigRepository;
    private final ManagedChannel projectServiceChannel;
    private final DisbursementRepository disbursementRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public DisbursementCalculationResponse calculatePreview(UUID milestoneId, BigDecimal totalAmount) {
        SystemConfig systemConfig = systemConfigRepository
                .findByName("fee-distribution")
                .orElseThrow(() -> new BusinessException("Không tìm thấy cấu hình fee-distribution"));

        Map<String, Object> properties = systemConfig.getProperties();
        BigDecimal talentShareRate = new BigDecimal(properties.get("talentShareRate").toString());
        BigDecimal mentorShareRate = new BigDecimal(properties.get("mentorShareRate").toString());
        BigDecimal systemFeeRate = new BigDecimal(properties.get("systemFeeRate").toString());

        BigDecimal systemFee = totalAmount.multiply(systemFeeRate);
        BigDecimal mentorShare = totalAmount.multiply(mentorShareRate);
        BigDecimal talentShare = totalAmount.multiply(talentShareRate);

        ProjectServiceGrpc.ProjectServiceBlockingStub projectServiceBlockingStub = ProjectServiceGrpc.newBlockingStub(projectServiceChannel);
        GetLeaderInfoResponse leaderInfoResponse = projectServiceBlockingStub.getLeaderInfo(GetLeaderInfoRequest
                .newBuilder()
                .setMilestoneId(milestoneId.toString())
                .build());

        LeaderInfo mentor = null;
        LeaderInfo talent = null;

        for (LeaderInfo leader : leaderInfoResponse.getLeadersList()) {
            if (Role.MENTOR.toString().equals(leader.getRoleInProject())) {
                mentor = leader;
            } else if (Role.TALENT.toString().equals(leader.getRoleInProject())) {
                talent = leader;
            }
        }

        // Nếu không tìm thấy mentor hoặc talent thì throw exception
        if (mentor == null) {
            throw new BusinessException("Không tìm thấy leader MENTOR cho milestone");
        }
        if (talent == null) {
            throw new BusinessException("Không tìm thấy leader TALENT cho milestone");
        }

        // Tạo đối tượng LeaderDisbursementInfo cho MENTOR
        LeaderDisbursementInfo mentorObj = LeaderDisbursementInfo.builder()
                .userId(mentor.getUserId())
                .fullName(mentor.getFullName())
                .email(mentor.getEmail())
                .avatarUrl(mentor.getAvatarUrl())
                .isLeader(true)
                .roleInProject(Role.MENTOR.toString())
                .amount(mentorShare)
                .build();

        // Tạo đối tượng LeaderDisbursementInfo cho TALENT
        LeaderDisbursementInfo talentObj = LeaderDisbursementInfo.builder()
                .userId(talent.getUserId())
                .fullName(talent.getFullName())
                .email(talent.getEmail())
                .avatarUrl(talent.getAvatarUrl())
                .isLeader(true)
                .roleInProject(Role.TALENT.toString())
                .amount(talentShare)
                .build();

        // Trả về DisbursementCalculationResponse
        return DisbursementCalculationResponse.builder()
                .milestoneId(milestoneId.toString())
                .totalAmount(totalAmount)
                .systemFee(systemFee)
                .mentorLeader(mentorObj)
                .talentLeader(talentObj)
                .status(Status.PENDING.toString())
                .build();
    }

    @Override
    public void calculateDisbursement(CreateDisbursementRequest request) {
        SystemConfig systemConfig = systemConfigRepository
                .findByName("fee-distribution")
                .orElseThrow(() -> new BusinessException("Không tìm thấy cấu hình fee-distribution"));

        Map<String, Object> properties = systemConfig.getProperties();
        BigDecimal talentShareRate = new BigDecimal(properties.get("talentShareRate").toString());
        BigDecimal mentorShareRate = new BigDecimal(properties.get("mentorShareRate").toString());
        BigDecimal systemFeeRate = new BigDecimal(properties.get("systemFeeRate").toString());

        BigDecimal systemFee = request.getTotalAmount().multiply(systemFeeRate);
        BigDecimal mentorShare = request.getTotalAmount().multiply(mentorShareRate);
        BigDecimal talentShare = request.getTotalAmount().multiply(talentShareRate);

        Disbursement disbursement = Disbursement
                .builder()
                .projectId(request.getProjectId())
                .milestoneId(request.getMilestoneId())
                .totalAmount(request.getTotalAmount())
                .systemFee(systemFee)
                .mentorLeaderId(request.getMentorLeaderId())
                .mentorAmount(mentorShare)
                .talentLeaderId(request.getTalentLeaderId())
                .talentAmount(talentShare)
                .status(Status.PENDING.toString())
                .build();

        disbursementRepository.save(disbursement);
    }

    @Override
    public DisbursementCalculationResponse executeDisbursement(UUID disbursementId) {
        Disbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy kết quả để thực thi phân bổ tiền với id: " + disbursementId));

        if (!Status.PENDING.toString().equals(disbursement.getStatus())) {
            throw new BusinessException("Tiền đã được phân bổ về ví các team leader");
        }

        ProjectServiceGrpc.ProjectServiceBlockingStub projectServiceBlockingStub = ProjectServiceGrpc.newBlockingStub(projectServiceChannel);
        GetLeaderInfoResponse leaderInfoResponse = projectServiceBlockingStub.getLeaderInfo(GetLeaderInfoRequest
                .newBuilder()
                .setMilestoneId(disbursement.getMilestoneId().toString())
                .build());

        Wallet systemWallet = walletRepository.findByOwnerType("SYSTEM").orElseThrow();
        // Trừ ví System (Tổng Mentor + Talent)
        BigDecimal totalOut = disbursement.getMentorAmount().add(disbursement.getTalentAmount());
        systemWallet.setBalance(systemWallet.getBalance().subtract(totalOut));

        // Cộng ví Mentor Leader
        Wallet mentorWallet = getOrCreateWallet(disbursement.getMentorLeaderId());
        mentorWallet.setBalance(mentorWallet.getBalance().add(disbursement.getMentorAmount()));
        createTransaction(mentorWallet, disbursement.getMentorAmount(), "CREDIT", "DISBURSEMENT_IN",
                "Nhận tiền giải ngân Milestone", disbursement.getId(), "DISBURSEMENT",
                disbursement.getProjectId(), disbursement.getMilestoneId());

        // Cộng ví Talent Leader
        Wallet talentWallet = getOrCreateWallet(disbursement.getTalentLeaderId());
        talentWallet.setBalance(talentWallet.getBalance().add(disbursement.getTalentAmount()));
        createTransaction(talentWallet, disbursement.getTalentAmount(), "CREDIT", "DISBURSEMENT_IN",
                "Nhận tiền giải ngân Milestone", disbursement.getId(), "DISBURSEMENT",
                disbursement.getProjectId(), disbursement.getMilestoneId());

        disbursement.setStatus(Status.COMPLETED.toString());
        disbursementRepository.save(disbursement);
        walletRepository.save(systemWallet);
        walletRepository.save(mentorWallet);
        walletRepository.save(talentWallet);

        LeaderInfo mentor = null;
        LeaderInfo talent = null;

        for (LeaderInfo leader : leaderInfoResponse.getLeadersList()) {
            if (Role.MENTOR.toString().equals(leader.getRoleInProject())) {
                mentor = leader;
            } else if (Role.TALENT.toString().equals(leader.getRoleInProject())) {
                talent = leader;
            }
        }

        // Nếu không tìm thấy mentor hoặc talent thì throw exception
        if (mentor == null) {
            throw new BusinessException("Không tìm thấy leader MENTOR cho milestone");
        }
        if (talent == null) {
            throw new BusinessException("Không tìm thấy leader TALENT cho milestone");
        }

        // Tạo đối tượng LeaderDisbursementInfo cho MENTOR
        LeaderDisbursementInfo mentorObj = LeaderDisbursementInfo.builder()
                .userId(mentor.getUserId())
                .fullName(mentor.getFullName())
                .email(mentor.getEmail())
                .avatarUrl(mentor.getAvatarUrl())
                .isLeader(true)
                .roleInProject(Role.MENTOR.toString())
                .amount(disbursement.getMentorAmount())
                .build();

        // Tạo đối tượng LeaderDisbursementInfo cho TALENT
        LeaderDisbursementInfo talentObj = LeaderDisbursementInfo.builder()
                .userId(talent.getUserId())
                .fullName(talent.getFullName())
                .email(talent.getEmail())
                .avatarUrl(talent.getAvatarUrl())
                .isLeader(true)
                .roleInProject(Role.TALENT.toString())
                .amount(disbursement.getTalentAmount())
                .build();

        return DisbursementCalculationResponse.builder()
                .milestoneId(disbursement.getMilestoneId().toString())
                .totalAmount(disbursement.getTotalAmount())
                .systemFee(disbursement.getSystemFee())
                .mentorLeader(mentorObj)
                .talentLeader(talentObj)
                .status(disbursement.getStatus())
                .build();
    }

    private Wallet getOrCreateWallet(UUID userId) {
        return walletRepository.findByOwnerId(userId)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setOwnerId(userId);
                    w.setOwnerType(Role.COMPANY.toString()); // Mặc định là COMPANY nếu là người thanh toán
                    w.setBalance(BigDecimal.ZERO);
                    w.setHeldBalance(BigDecimal.ZERO);
                    w.setCurrency("VND");
                    w.setStatus(Status.ACTIVE.toString());
                    return walletRepository.save(w);
                });
    }

    private Wallet getOrCreateSystemWallet() {
        return walletRepository.findByOwnerType("SYSTEM")
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setOwnerType("SYSTEM");
                    w.setOwnerId(UUID.randomUUID()); // ID ảo
                    w.setBalance(BigDecimal.ZERO);
                    w.setHeldBalance(BigDecimal.ZERO);
                    w.setCurrency("VND");
                    w.setStatus(Status.ACTIVE.toString());
                    return walletRepository.save(w);
                });
    }

    private void createTransaction(Wallet wallet, BigDecimal amount, String type, String direction,
                                   String desc, UUID refId, String refType,
                                   UUID projectId, UUID milestoneId) {
        Transaction tx = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(type)
                .direction(direction)
                .description(desc)
                .refId(refId).refType(refType)
                .projectId(projectId).milestoneId(milestoneId)
                .status(Status.SUCCESS.toString())
                .balanceAfter(wallet.getBalance())
                .build();
        transactionRepository.save(tx);
    }
}
